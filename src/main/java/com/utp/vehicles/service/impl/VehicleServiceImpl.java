package com.utp.vehicles.service.impl;

import com.utp.vehicles.generated.model.VehicleDetail;
import com.utp.vehicles.generated.model.VehicleDetailList;
import com.utp.vehicles.generated.model.VehicleIn;
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
    return vehicleRepository
        .findAllByIdUserAndIdVehicleStatusOrderByIdVehicle(authenticatedUserId.intValue(),
            Constants.ID_VEHICLE_STATUS_ASSIGNED)
        .collectList()
        .flatMap(this::toDetailList);
  }

  @Override
  public Mono<VehicleDetail> registerVehicle(Long authenticatedUserId, VehicleIn vehicle) {
    Integer userId = authenticatedUserId.intValue();
    String numberPlate = NumberPlateValidator.normalize(vehicle.getNumberPlate());

    return NumberPlateValidator.validate(numberPlate, vehicle.getVehicleType())
        .then(Mono.defer(() -> validatePlateIsAvailable(userId, numberPlate)))
        .then(Mono.defer(() -> validateAssignedVehicleLimit(userId)))
        .then(Mono.defer(() -> createVehicle(userId, numberPlate, vehicle.getVehicleType())))
        .as(transactionalOperator::transactional)
        .flatMap(this::toDetail);
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

  private Mono<Void> validateAssignedVehicleLimit(Integer userId) {
    return vehicleRepository
        .countByIdUserAndIdVehicleStatus(userId, Constants.ID_VEHICLE_STATUS_ASSIGNED)
        .defaultIfEmpty(0L)
        .flatMap(assigned -> assigned >= Constants.MAX_ASSIGNED_VEHICLES_PER_USER
            ? Mono.error(new ConflictException(Constants.ERROR_MAX_ASSIGNED_VEHICLES_REACHED))
            : Mono.empty());
  }

  private Mono<Vehicle> createVehicle(Integer userId, String numberPlate, Integer idVehicleType) {
    return vehicleRepository
        .insertVehicle(idVehicleType, userId, numberPlate, Constants.ID_VEHICLE_STATUS_ASSIGNED)
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

    return new VehicleDetailList()
        .vehicles(details)
        .assignedVehicles(details.size())
        .maxAssignedVehicles(Constants.MAX_ASSIGNED_VEHICLES_PER_USER);
  }

  private VehicleDetailList emptyList() {
    return new VehicleDetailList()
        .vehicles(List.of())
        .assignedVehicles(0)
        .maxAssignedVehicles(Constants.MAX_ASSIGNED_VEHICLES_PER_USER);
  }
}
