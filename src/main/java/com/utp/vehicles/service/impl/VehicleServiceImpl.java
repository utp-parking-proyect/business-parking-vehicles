package com.utp.vehicles.service.impl;

import com.utp.vehicles.generated.model.VehicleAvailabilityIn;
import com.utp.vehicles.generated.model.VehicleDetail;
import com.utp.vehicles.generated.model.VehicleDetailList;
import com.utp.vehicles.generated.model.VehicleIn;
import com.utp.vehicles.generated.model.VehicleStatus;
import com.utp.vehicles.mapper.VehicleInformationMapper;
import com.utp.vehicles.model.entity.Vehicle;
import com.utp.vehicles.model.entity.VehicleType;
import com.utp.vehicles.repository.VehicleRepository;
import com.utp.vehicles.repository.VehicleTypeRepository;
import com.utp.vehicles.service.VehicleService;
import com.utp.vehicles.util.Constants;
import com.utp.vehicles.util.NumberPlateValidator;
import com.utp.vehicles.util.error.ConflictException;
import com.utp.vehicles.util.error.ForbiddenException;
import com.utp.vehicles.util.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

  private final VehicleRepository vehicleRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final TransactionalOperator transactionalOperator;
  private final VehicleInformationMapper vehicleInformationMapper;

  @Override
  public Mono<VehicleDetailList> getMyVehicles(Long authenticatedUserId) {
    return vehicleRepository.findAllByIdUserOrderByIdVehicle(authenticatedUserId.intValue())
        .collectList()
        .flatMap(this::toDetailList);
  }

  @Override
  public Mono<VehicleDetail> registerVehicle(Long authenticatedUserId, VehicleIn vehicle) {
    Integer userId = authenticatedUserId.intValue();
    String numberPlate = NumberPlateValidator.normalize(vehicle.getNumberPlate());

    return NumberPlateValidator.validate(numberPlate, vehicle.getVehicleType())
        .then(Mono.defer(() -> validatePlateIsAvailable(userId, numberPlate)))
        .then(Mono.defer(() -> validateActiveVehicleLimit(userId,
            Constants.ERROR_MAX_ACTIVE_VEHICLES_REACHED)))
        .then(Mono.defer(() -> createVehicle(userId, numberPlate, vehicle.getVehicleType())))
        .as(transactionalOperator::transactional)
        .flatMap(this::toDetail);
  }

  @Override
  public Mono<VehicleDetail> updateVehicleAvailability(Long authenticatedUserId, Integer vehicleId,
                                                       VehicleAvailabilityIn availability) {
    if (availability == null || availability.getActive() == null) {
      return Mono.error(new IllegalArgumentException(Constants.ERROR_VEHICLE_ACTIVE_REQUIRED));
    }

    Integer target = Boolean.TRUE.equals(availability.getActive())
        ? Constants.ID_VEHICLE_STATUS_ACTIVE
        : Constants.ID_VEHICLE_STATUS_DISABLED;

    return findOwnedVehicle(authenticatedUserId, vehicleId)
        .flatMap(vehicle -> {
          if (target.equals(vehicle.getIdVehicleStatus())) {
            return Mono.just(vehicle);
          }
          return validateEnabling(vehicle.getIdUser(), target)
              .then(Mono.defer(() -> {
                log.info("Updating status of vehicle {} to {}", vehicleId, target);
                return vehicleRepository.updateStatus(vehicleId, target)
                    .then(Mono.defer(() -> vehicleRepository.findById(vehicleId)));
              }));
        })
        .as(transactionalOperator::transactional)
        .flatMap(this::toDetail);
  }

  private Mono<Void> validateEnabling(Integer userId, Integer targetStatus) {
    return Constants.ID_VEHICLE_STATUS_ACTIVE.equals(targetStatus)
        ? validateActiveVehicleLimit(userId, Constants.ERROR_MAX_ACTIVE_VEHICLES_TO_ENABLE)
        : Mono.empty();
  }

  private Mono<Vehicle> findOwnedVehicle(Long authenticatedUserId, Integer vehicleId) {
    return vehicleRepository.findById(vehicleId)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_VEHICLE_NOT_FOUND)))
        .flatMap(vehicle -> {
          if (Constants.ID_VEHICLE_STATUS_UNASSIGNED.equals(vehicle.getIdVehicleStatus())) {
            return Mono.error(new ConflictException(Constants.ERROR_VEHICLE_ALREADY_UNASSIGNED));
          }
          return isOwnedBy(vehicle, authenticatedUserId)
              ? Mono.just(vehicle)
              : Mono.error(new ForbiddenException(Constants.ERROR_VEHICLE_NOT_OWNED));
        });
  }

  private boolean isOwnedBy(Vehicle vehicle, Long authenticatedUserId) {
    return vehicle.getIdUser() != null && authenticatedUserId.equals(vehicle.getIdUser().longValue());
  }

  private Mono<Void> validatePlateIsAvailable(Integer userId, String numberPlate) {
    return vehicleRepository.findByNumberPlate(numberPlate)
        .flatMap(existing -> Mono.error(plateConflict(userId, existing)))
        .then();
  }

  private RuntimeException plateConflict(Integer userId, Vehicle existing) {
    if (existing.getIdUser() == null) {
      return new ConflictException(Constants.ERROR_PLATE_REGISTERED_UNASSIGNED);
    }
    return existing.getIdUser().equals(userId)
        ? new ConflictException(Constants.ERROR_VEHICLE_ALREADY_REGISTERED)
        : new ForbiddenException(Constants.ERROR_VEHICLE_OWNED_BY_ANOTHER_USER);
  }

  private Mono<Void> validateActiveVehicleLimit(Integer userId, String errorMessage) {
    return vehicleRepository
        .countByIdUserAndIdVehicleStatus(userId, Constants.ID_VEHICLE_STATUS_ACTIVE)
        .defaultIfEmpty(0L)
        .flatMap(active -> active >= Constants.MAX_ACTIVE_VEHICLES_PER_USER
            ? Mono.error(new ConflictException(errorMessage))
            : Mono.empty());
  }

  private Mono<Vehicle> createVehicle(Integer userId, String numberPlate, Integer idVehicleType) {
    return vehicleRepository
        .insertVehicle(idVehicleType, userId, numberPlate, Constants.ID_VEHICLE_STATUS_ACTIVE)
        .flatMap(vehicleRepository::findById);
  }

  private Mono<VehicleDetail> toDetail(Vehicle vehicle) {
    return vehicleTypeRepository.findById(vehicle.getIdVehicleType())
        .map(vehicleType -> vehicleInformationMapper.toVehicleDetail(vehicle, vehicleType))
        .defaultIfEmpty(vehicleInformationMapper.toVehicleDetail(vehicle, null));
  }

  private Mono<VehicleDetailList> toDetailList(List<Vehicle> vehicles) {
    if (vehicles.isEmpty()) {
      return Mono.just(emptyList());
    }

    Set<Integer> vehicleTypeIds = vehicles.stream()
        .map(Vehicle::getIdVehicleType).collect(Collectors.toSet());

    return vehicleTypeRepository.findAllById(vehicleTypeIds)
        .collectMap(VehicleType::getIdVehicleType)
        .map(vehicleTypes -> toDetailList(vehicles, vehicleTypes));
  }

  private VehicleDetailList toDetailList(List<Vehicle> vehicles, Map<Integer, VehicleType> vehicleTypes) {
    List<VehicleDetail> details = vehicles.stream()
        .map(vehicle -> vehicleInformationMapper
            .toVehicleDetail(vehicle, vehicleTypes.get(vehicle.getIdVehicleType())))
        .toList();

    int active = (int) details.stream()
        .filter(detail -> VehicleStatus.ACTIVE.equals(detail.getStatus())).count();

    return new VehicleDetailList()
        .vehicles(details)
        .assignedVehicles(details.size())
        .activeVehicles(active)
        .maxActiveVehicles(Constants.MAX_ACTIVE_VEHICLES_PER_USER);
  }

  private VehicleDetailList emptyList() {
    return new VehicleDetailList()
        .vehicles(List.of())
        .assignedVehicles(0)
        .activeVehicles(0)
        .maxActiveVehicles(Constants.MAX_ACTIVE_VEHICLES_PER_USER);
  }
}
