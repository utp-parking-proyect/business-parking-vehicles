package com.utp.vehicles.service.impl;

import com.utp.vehicles.client.portal.PortalServiceClient;
import com.utp.vehicles.generated.client.users.model.UserResponse;
import com.utp.vehicles.generated.model.ParkingResponseIn;
import com.utp.vehicles.generated.model.VehicleUnassignmentIn;
import com.utp.vehicles.generated.model.VehicleUnassignmentRequestDetail;
import com.utp.vehicles.generated.model.VehicleUnassignmentRequestList;
import com.utp.vehicles.generated.model.WorkflowEntry;
import com.utp.vehicles.mapper.VehicleInformationMapper;
import com.utp.vehicles.model.entity.Status;
import com.utp.vehicles.model.entity.Vehicle;
import com.utp.vehicles.model.entity.VehicleType;
import com.utp.vehicles.model.entity.VehicleUnassignmentRequest;
import com.utp.vehicles.repository.StatusRepository;
import com.utp.vehicles.repository.VehicleRepository;
import com.utp.vehicles.repository.VehicleTypeRepository;
import com.utp.vehicles.repository.VehicleUnassignmentRequestRepository;
import com.utp.vehicles.repository.VehicleUnassignmentWorkflowRepository;
import com.utp.vehicles.service.AcceptorSelector;
import com.utp.vehicles.service.VehicleUnassignmentService;
import com.utp.vehicles.service.WorkflowService;
import com.utp.vehicles.util.Constants;
import com.utp.vehicles.util.error.ConflictException;
import com.utp.vehicles.util.error.ForbiddenException;
import com.utp.vehicles.util.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleUnassignmentServiceImpl implements VehicleUnassignmentService {

  private final VehicleUnassignmentRequestRepository unassignmentRepository;
  private final VehicleRepository vehicleRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final VehicleUnassignmentWorkflowRepository unassignmentWorkflowRepository;
  private final StatusRepository statusRepository;
  private final PortalServiceClient portalServiceClient;
  private final AcceptorSelector acceptorSelector;
  private final WorkflowService workflowService;
  private final TransactionalOperator transactionalOperator;
  private final VehicleInformationMapper vehicleInformationMapper;

  @Override
  public Mono<VehicleUnassignmentRequestDetail> requestUnassignment(Long authenticatedUserId,
                                                                    Integer vehicleId,
                                                                    VehicleUnassignmentIn unassignment) {
    if (unassignment == null || unassignment.getReason() == null
        || unassignment.getReason().isBlank()) {
      return Mono.error(new IllegalArgumentException(Constants.ERROR_UNASSIGNMENT_REASON_REQUIRED));
    }

    Integer userId = authenticatedUserId.intValue();
    String reason = unassignment.getReason().trim();

    return portalServiceClient.getUserById(authenticatedUserId)
        .flatMap(applicant -> createUnassignment(userId, vehicleId, reason, resolveCampusId(applicant))
            .as(transactionalOperator::transactional))
        .flatMap(this::toDetail);
  }

  private Mono<VehicleUnassignmentRequest> createUnassignment(Integer userId, Integer vehicleId,
                                                              String reason, Long idCampus) {
    return findOwnedVehicle(userId, vehicleId)
        .flatMap(vehicle -> validateNoOpenUnassignment(vehicleId))
        .then(Mono.defer(() -> {
          LocalDateTime now = LocalDateTime.now();
          return unassignmentRepository
              .insertUnassignmentRequest(vehicleId, userId, Constants.ID_STATUS_REGISTERED, reason, now)
              .flatMap(unassignmentId -> unassignmentWorkflowRepository
                  .saveWorkflow(unassignmentId, Constants.ID_STATUS_REGISTERED, now,
                      Constants.OBSERVATION_UNASSIGNMENT_REGISTERED)
                  .then(Mono.defer(() -> assignAcceptor(unassignmentId, idCampus)))
                  .then(Mono.defer(() -> unassignmentRepository.findById(unassignmentId))));
        }));
  }

  private Mono<Vehicle> findOwnedVehicle(Integer userId, Integer vehicleId) {
    return vehicleRepository.findById(vehicleId)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_VEHICLE_NOT_FOUND)))
        .flatMap(vehicle -> {
          if (Constants.ID_VEHICLE_STATUS_UNASSIGNED.equals(vehicle.getIdVehicleStatus())
              || vehicle.getIdUser() == null) {
            return Mono.error(new ConflictException(Constants.ERROR_VEHICLE_ALREADY_UNASSIGNED));
          }
          return userId.equals(vehicle.getIdUser())
              ? Mono.just(vehicle)
              : Mono.error(new ForbiddenException(Constants.ERROR_VEHICLE_NOT_OWNED));
        });
  }

  private Mono<Void> validateNoOpenUnassignment(Integer vehicleId) {
    return unassignmentRepository
        .findFirstByIdVehicleAndIdStatusIn(vehicleId, Constants.ID_STATUSES_OPEN)
        .flatMap(existing -> Mono.error(
            new ConflictException(Constants.ERROR_UNASSIGNMENT_ALREADY_IN_PROGRESS)))
        .then();
  }

  private Mono<Void> assignAcceptor(Integer unassignmentId, Long idCampus) {
    return acceptorSelector.selectLeastLoaded(idCampus)
        .flatMap(acceptorId -> {
          log.info("Assigning acceptor {} to unassignment request {}", acceptorId, unassignmentId);
          return unassignmentRepository
              .updateAcceptorAndStatus(unassignmentId, acceptorId, Constants.ID_STATUS_IN_REVISION)
              .then(unassignmentWorkflowRepository.saveWorkflow(unassignmentId,
                  Constants.ID_STATUS_IN_REVISION, LocalDateTime.now(),
                  Constants.OBSERVATION_IN_REVISION));
        });
  }

  @Override
  public Mono<VehicleUnassignmentRequestList> getMyUnassignmentRequests(Long authenticatedUserId) {
    return unassignmentRepository
        .findAllByIdApplicantOrderByIdUnassignmentRequestDesc(authenticatedUserId.intValue())
        .collectList()
        .flatMap(this::toList);
  }

  @Override
  public Mono<VehicleUnassignmentRequestList> getUnassignmentRequestsByAcceptor(
      Long authenticatedUserId, Integer acceptorId) {
    if (acceptorId == null || !authenticatedUserId.equals(acceptorId.longValue())) {
      return Mono.error(new ForbiddenException(Constants.ERROR_NOT_ACCEPTOR));
    }

    return portalServiceClient.getUserById(authenticatedUserId)
        .switchIfEmpty(Mono.error(new ForbiddenException(Constants.ERROR_USER_NOT_SAE)))
        .flatMap(this::validateSaeRole)
        .thenMany(unassignmentRepository
            .findAllByIdAcceptorOrderByIdUnassignmentRequestDesc(acceptorId))
        .collectList()
        .flatMap(this::toList);
  }

  @Override
  public Mono<VehicleUnassignmentRequestDetail> respondToUnassignmentRequest(
      Long authenticatedUserId, Integer unassignmentRequestId, ParkingResponseIn response) {
    return validateResponseBody(response)
        .then(Mono.defer(() -> portalServiceClient.getUserById(authenticatedUserId)
            .switchIfEmpty(Mono.error(new ForbiddenException(Constants.ERROR_USER_NOT_SAE)))
            .flatMap(this::validateSaeRole)
            .then(Mono.defer(() -> applyResponse(authenticatedUserId, unassignmentRequestId, response)
                .as(transactionalOperator::transactional)))))
        .flatMap(this::toDetail);
  }

  private Mono<Void> validateResponseBody(ParkingResponseIn response) {
    if (response == null || response.getApproved() == null) {
      return Mono.error(new IllegalArgumentException(Constants.ERROR_APPROVED_REQUIRED));
    }
    if (!response.getApproved() && (response.getComment() == null || response.getComment().isBlank())) {
      return Mono.error(new IllegalArgumentException(Constants.ERROR_COMMENT_REQUIRED_ON_REJECTION));
    }
    return Mono.empty();
  }

  private Mono<Void> validateSaeRole(UserResponse user) {
    return portalServiceClient.hasSaeRole(user)
        ? Mono.empty()
        : Mono.error(new ForbiddenException(Constants.ERROR_USER_NOT_SAE));
  }

  private Mono<VehicleUnassignmentRequest> applyResponse(Long authenticatedUserId,
                                                         Integer unassignmentRequestId,
                                                         ParkingResponseIn response) {
    boolean approved = Boolean.TRUE.equals(response.getApproved());
    Integer newStatus = approved ? Constants.ID_STATUS_APPROVED : Constants.ID_STATUS_REJECTED;
    LocalDateTime now = LocalDateTime.now();

    return unassignmentRepository.findById(unassignmentRequestId)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_UNASSIGNMENT_NOT_FOUND)))
        .flatMap(unassignment -> validateAcceptor(unassignment, authenticatedUserId)
            .then(Mono.defer(() -> validateInReview(unassignment)))
            .then(Mono.defer(() -> unassignmentRepository
                .updateStatusAndResponse(unassignmentRequestId, newStatus, now)))
            .then(Mono.defer(() -> unassignmentWorkflowRepository.saveWorkflow(unassignmentRequestId,
                newStatus, now, resolveObservation(response, approved))))
            .then(Mono.defer(() -> approved
                ? unassignVehicle(unassignment.getIdVehicle())
                : Mono.empty()))
            .then(Mono.defer(() -> unassignmentRepository.findById(unassignmentRequestId))));
  }

  private Mono<Void> unassignVehicle(Integer vehicleId) {
    log.info("Unassigning vehicle {}", vehicleId);
    return vehicleRepository.unassignVehicle(vehicleId, Constants.ID_VEHICLE_STATUS_UNASSIGNED);
  }

  private Mono<Void> validateAcceptor(VehicleUnassignmentRequest unassignment,
                                      Long authenticatedUserId) {
    return unassignment.getIdAcceptor() != null
        && authenticatedUserId.equals(unassignment.getIdAcceptor().longValue())
        ? Mono.empty()
        : Mono.error(new ForbiddenException(Constants.ERROR_NOT_ACCEPTOR));
  }

  private Mono<Void> validateInReview(VehicleUnassignmentRequest unassignment) {
    return Constants.ID_STATUS_IN_REVISION.equals(unassignment.getIdStatus())
        ? Mono.empty()
        : Mono.error(new ConflictException(Constants.ERROR_UNASSIGNMENT_NOT_IN_REVIEW));
  }

  private String resolveObservation(ParkingResponseIn response, boolean approved) {
    if (response.getComment() != null && !response.getComment().isBlank()) {
      return response.getComment();
    }
    return approved
        ? Constants.OBSERVATION_UNASSIGNMENT_APPROVED_DEFAULT
        : Constants.OBSERVATION_UNASSIGNMENT_REJECTED_DEFAULT;
  }

  private Long resolveCampusId(UserResponse applicant) {
    return applicant.getCampus() == null ? null : applicant.getCampus().getIdCampus();
  }

  private Mono<VehicleUnassignmentRequestDetail> toDetail(VehicleUnassignmentRequest unassignment) {
    return toList(List.of(unassignment))
        .map(list -> list.getUnassignmentRequests().getFirst());
  }

  private Mono<VehicleUnassignmentRequestList> toList(List<VehicleUnassignmentRequest> unassignments) {
    if (unassignments.isEmpty()) {
      return Mono.just(new VehicleUnassignmentRequestList().unassignmentRequests(List.of()));
    }

    Set<Integer> vehicleIds = unassignments.stream()
        .map(VehicleUnassignmentRequest::getIdVehicle).collect(Collectors.toSet());
    Set<Integer> statusIds = unassignments.stream()
        .map(VehicleUnassignmentRequest::getIdStatus).collect(Collectors.toSet());
    Set<Long> applicantIds = unassignments.stream()
        .map(unassignment -> unassignment.getIdApplicant().longValue()).collect(Collectors.toSet());

    return Mono.zip(
            vehicleRepository.findAllById(vehicleIds).collectMap(Vehicle::getIdVehicle),
            statusRepository.findAllById(statusIds).collectMap(Status::getIdStatus),
            Flux.fromIterable(applicantIds).flatMap(portalServiceClient::getUserById)
                .collectMap(UserResponse::getIdUser))
        .flatMap(tuple -> buildList(unassignments, tuple.getT1(), tuple.getT2(), tuple.getT3()));
  }

  private Mono<VehicleUnassignmentRequestList> buildList(
      List<VehicleUnassignmentRequest> unassignments, Map<Integer, Vehicle> vehicles,
      Map<Integer, Status> statuses, Map<Long, UserResponse> applicants) {

    Set<Integer> vehicleTypeIds = vehicles.values().stream()
        .map(Vehicle::getIdVehicleType).collect(Collectors.toSet());

    return vehicleTypeRepository.findAllById(vehicleTypeIds)
        .collectMap(VehicleType::getIdVehicleType)
        .flatMap(vehicleTypes -> Flux.fromIterable(unassignments)
            .concatMap(unassignment -> buildWorkflow(unassignment.getIdUnassignmentRequest())
                .map(workflow -> toDetail(unassignment, vehicles, statuses, applicants,
                    vehicleTypes, workflow)))
            .collectList()
            .map(details -> new VehicleUnassignmentRequestList().unassignmentRequests(details)));
  }

  private VehicleUnassignmentRequestDetail toDetail(VehicleUnassignmentRequest unassignment,
                                                    Map<Integer, Vehicle> vehicles,
                                                    Map<Integer, Status> statuses,
                                                    Map<Long, UserResponse> applicants,
                                                    Map<Integer, VehicleType> vehicleTypes,
                                                    List<WorkflowEntry> workflow) {
    Vehicle vehicle = vehicles.get(unassignment.getIdVehicle());
    VehicleType vehicleType = vehicle == null ? null : vehicleTypes.get(vehicle.getIdVehicleType());
    Status status = statuses.get(unassignment.getIdStatus());
    UserResponse applicant = applicants.get(unassignment.getIdApplicant().longValue());

    return new VehicleUnassignmentRequestDetail()
        .idUnassignmentRequest(unassignment.getIdUnassignmentRequest())
        .idVehicle(unassignment.getIdVehicle())
        .applicant(vehicleInformationMapper.toApplicantInformation(applicant, null))
        .vehicle(vehicleInformationMapper.toVehicleInformation(vehicle, vehicleType))
        .reason(unassignment.getReason())
        .status(status == null ? null : status.getNameStatus())
        .dateRequest(unassignment.getDateRequest() == null
            ? null : unassignment.getDateRequest().toString())
        .dateResponse(unassignment.getDateResponse() == null
            ? null : unassignment.getDateResponse().toString())
        .workflow(workflow);
  }

  private Mono<List<WorkflowEntry>> buildWorkflow(Integer unassignmentRequestId) {
    return workflowService.toEntries(
        unassignmentWorkflowRepository.findAllByUnassignmentRequestId(unassignmentRequestId));
  }
}
