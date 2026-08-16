package com.utp.vehicles.service.impl;

import com.utp.vehicles.client.portal.PortalServiceClient;
import com.utp.vehicles.generated.client.users.model.CampusResponse;
import com.utp.vehicles.generated.client.users.model.Role;
import com.utp.vehicles.generated.client.users.model.UserResponse;
import com.utp.vehicles.generated.model.ParkingResponseIn;
import com.utp.vehicles.generated.model.VehicleUnassignmentIn;
import com.utp.vehicles.mapper.VehicleInformationMapperImpl;
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
import com.utp.vehicles.service.WorkflowService;
import com.utp.vehicles.util.Constants;
import com.utp.vehicles.util.error.ConflictException;
import com.utp.vehicles.util.error.ForbiddenException;
import com.utp.vehicles.util.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleUnassignmentServiceImplTest {

  private static final Long OWNER_ID = 10L;
  private static final Long ANOTHER_USER_ID = 99L;
  private static final Long ACCEPTOR_ID = 20L;
  private static final Long CAMPUS_ID = 7L;

  @Mock
  private VehicleUnassignmentRequestRepository unassignmentRepository;
  @Mock
  private VehicleRepository vehicleRepository;
  @Mock
  private VehicleTypeRepository vehicleTypeRepository;
  @Mock
  private VehicleUnassignmentWorkflowRepository unassignmentWorkflowRepository;
  @Mock
  private StatusRepository statusRepository;
  @Mock
  private PortalServiceClient portalServiceClient;
  @Mock
  private AcceptorSelector acceptorSelector;
  @Mock
  private TransactionalOperator transactionalOperator;

  @InjectMocks
  private VehicleUnassignmentServiceImpl unassignmentService;

  @BeforeEach
  void mockTransactionalOperator() {
    Mockito.lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
  }

  @BeforeEach
  void useRealWorkflowService() {
    ReflectionTestUtils.setField(unassignmentService, "workflowService",
        new WorkflowService(statusRepository));
  }

  @BeforeEach
  void useRealMapper() {
    ReflectionTestUtils.setField(unassignmentService, "vehicleInformationMapper",
        new VehicleInformationMapperImpl());
  }

  private Vehicle vehicle(Integer idVehicle, Integer idUser, Integer idVehicleStatus) {
    Vehicle vehicle = new Vehicle();
    vehicle.setIdVehicle(idVehicle);
    vehicle.setIdUser(idUser);
    vehicle.setIdVehicleType(1);
    vehicle.setNumberPlate("ABC-123");
    vehicle.setIdVehicleStatus(idVehicleStatus);
    return vehicle;
  }

  private VehicleType vehicleType() {
    VehicleType vehicleType = new VehicleType();
    vehicleType.setIdVehicleType(1);
    vehicleType.setNameVehicleType("Automóvil");
    return vehicleType;
  }

  private Status status(Integer idStatus, String name) {
    Status status = new Status();
    status.setIdStatus(idStatus);
    status.setNameStatus(name);
    return status;
  }

  private VehicleUnassignmentRequest unassignment(Integer id, Integer idVehicle, Integer idStatus,
                                                  Integer idAcceptor) {
    VehicleUnassignmentRequest unassignment = new VehicleUnassignmentRequest();
    unassignment.setIdUnassignmentRequest(id);
    unassignment.setIdVehicle(idVehicle);
    unassignment.setIdApplicant(OWNER_ID.intValue());
    unassignment.setIdAcceptor(idAcceptor);
    unassignment.setIdStatus(idStatus);
    unassignment.setReason("Vendí el vehículo.");
    unassignment.setDateRequest(LocalDateTime.of(2026, 6, 1, 10, 0));
    return unassignment;
  }

  private UserResponse applicant() {
    UserResponse user = new UserResponse();
    user.setIdUser(OWNER_ID);
    user.setUsername("U23201703");
    user.setName("Juan");
    user.setLastname("Pérez");
    CampusResponse campus = new CampusResponse();
    campus.setIdCampus(CAMPUS_ID);
    campus.setNameCampus("Campus Central");
    user.setCampus(campus);
    return user;
  }

  private UserResponse saeUser() {
    Role role = new Role();
    role.setIdRole(3L);
    role.setName("ROLE_SAE");
    UserResponse user = new UserResponse();
    user.setIdUser(ACCEPTOR_ID);
    user.setUsername("sae20");
    user.setName("Ana");
    user.setLastname("Torres");
    user.setRoles(List.of(role));
    return user;
  }

  private UserResponse studentUser() {
    Role role = new Role();
    role.setIdRole(1L);
    role.setName("ROLE_STUDENT");
    UserResponse user = new UserResponse();
    user.setIdUser(OWNER_ID);
    user.setUsername("U23201703");
    user.setName("Juan");
    user.setLastname("Pérez");
    user.setRoles(List.of(role));
    return user;
  }

  private void mockDetailLookups(Integer idStatus, String statusName) {
    when(vehicleRepository.findAllById(Set.of(1)))
        .thenReturn(Flux.just(vehicle(1, OWNER_ID.intValue(), Constants.ID_VEHICLE_STATUS_ASSIGNED)));
    when(statusRepository.findAllById(Set.of(idStatus)))
        .thenReturn(Flux.just(status(idStatus, statusName)));
    when(portalServiceClient.getUserById(OWNER_ID)).thenReturn(Mono.just(applicant()));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.just(vehicleType()));
    when(unassignmentWorkflowRepository.findAllByUnassignmentRequestId(anyInt())).thenReturn(Flux.empty());
  }

  @Test
  void testRequestUnassignment_OwnActiveVehicle_CreatesRequestAndAssignsAcceptor() {
    when(portalServiceClient.getUserById(OWNER_ID)).thenReturn(Mono.just(applicant()));
    when(vehicleRepository.findById(1))
        .thenReturn(Mono.just(vehicle(1, OWNER_ID.intValue(), Constants.ID_VEHICLE_STATUS_ASSIGNED)));
    when(unassignmentRepository.findFirstByIdVehicleAndIdStatusIn(1, Constants.ID_STATUSES_OPEN))
        .thenReturn(Mono.empty());
    when(unassignmentRepository.insertUnassignmentRequest(any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(50));
    when(unassignmentWorkflowRepository.saveWorkflow(any(), any(), any(), any()))
        .thenReturn(Mono.empty());
    when(acceptorSelector.selectLeastLoaded(CAMPUS_ID)).thenReturn(Mono.just(20));
    when(unassignmentRepository.updateAcceptorAndStatus(50, 20, Constants.ID_STATUS_IN_REVISION))
        .thenReturn(Mono.empty());
    when(unassignmentRepository.findById(50))
        .thenReturn(Mono.just(unassignment(50, 1, Constants.ID_STATUS_IN_REVISION, 20)));
    when(vehicleRepository.findAllById(Set.of(1)))
        .thenReturn(Flux.just(vehicle(1, OWNER_ID.intValue(), Constants.ID_VEHICLE_STATUS_ASSIGNED)));
    when(statusRepository.findAllById(Set.of(Constants.ID_STATUS_IN_REVISION)))
        .thenReturn(Flux.just(status(Constants.ID_STATUS_IN_REVISION, "EN_REVISION")));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.just(vehicleType()));
    when(unassignmentWorkflowRepository.findAllByUnassignmentRequestId(50)).thenReturn(Flux.empty());

    StepVerifier.create(unassignmentService.requestUnassignment(OWNER_ID, 1,
            new VehicleUnassignmentIn().reason("Vendí el vehículo.")))
        .assertNext(detail -> {
          assertEquals(50, detail.getIdUnassignmentRequest());
          assertEquals("ABC-123", detail.getVehicle().getNumberPlate());
          assertEquals("Vendí el vehículo.", detail.getReason());
          assertEquals("EN_REVISION", detail.getStatus());
        })
        .verifyComplete();

    Mockito.verify(vehicleRepository, Mockito.never()).unassignVehicle(anyInt(), anyInt());
  }

  @Test
  void testRequestUnassignment_AnotherUsersVehicle_IsForbidden() {
    when(portalServiceClient.getUserById(ANOTHER_USER_ID)).thenReturn(Mono.just(applicant()));
    when(vehicleRepository.findById(1))
        .thenReturn(Mono.just(vehicle(1, OWNER_ID.intValue(), Constants.ID_VEHICLE_STATUS_ASSIGNED)));

    StepVerifier.create(unassignmentService.requestUnassignment(ANOTHER_USER_ID, 1,
            new VehicleUnassignmentIn().reason("Vendí el vehículo.")))
        .expectErrorMatches(error -> error instanceof ForbiddenException
            && error.getMessage().equals(Constants.ERROR_VEHICLE_NOT_OWNED))
        .verify();

    Mockito.verify(unassignmentRepository, Mockito.never())
        .insertUnassignmentRequest(any(), any(), any(), any(), any());
  }

  @Test
  void testRequestUnassignment_AlreadyUnassignedVehicle_IsConflict() {
    when(portalServiceClient.getUserById(OWNER_ID)).thenReturn(Mono.just(applicant()));
    when(vehicleRepository.findById(1))
        .thenReturn(Mono.just(vehicle(1, null, Constants.ID_VEHICLE_STATUS_UNASSIGNED)));

    StepVerifier.create(unassignmentService.requestUnassignment(OWNER_ID, 1,
            new VehicleUnassignmentIn().reason("Vendí el vehículo.")))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().equals(Constants.ERROR_VEHICLE_ALREADY_UNASSIGNED))
        .verify();

    Mockito.verify(unassignmentRepository, Mockito.never())
        .insertUnassignmentRequest(any(), any(), any(), any(), any());
  }

  @Test
  void testRequestUnassignment_WithOpenRequest_IsConflict() {
    when(portalServiceClient.getUserById(OWNER_ID)).thenReturn(Mono.just(applicant()));
    when(vehicleRepository.findById(1))
        .thenReturn(Mono.just(vehicle(1, OWNER_ID.intValue(), Constants.ID_VEHICLE_STATUS_ASSIGNED)));
    when(unassignmentRepository.findFirstByIdVehicleAndIdStatusIn(1, Constants.ID_STATUSES_OPEN))
        .thenReturn(Mono.just(unassignment(50, 1, Constants.ID_STATUS_IN_REVISION, 20)));

    StepVerifier.create(unassignmentService.requestUnassignment(OWNER_ID, 1,
            new VehicleUnassignmentIn().reason("Vendí el vehículo.")))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().equals(Constants.ERROR_UNASSIGNMENT_ALREADY_IN_PROGRESS))
        .verify();

    Mockito.verify(unassignmentRepository, Mockito.never())
        .insertUnassignmentRequest(any(), any(), any(), any(), any());
  }

  @Test
  void testRequestUnassignment_WithoutReason_IsInvalid() {
    StepVerifier.create(unassignmentService.requestUnassignment(OWNER_ID, 1,
            new VehicleUnassignmentIn().reason("   ")))
        .expectErrorMatches(error -> error instanceof IllegalArgumentException
            && error.getMessage().equals(Constants.ERROR_UNASSIGNMENT_REASON_REQUIRED))
        .verify();

    Mockito.verifyNoInteractions(unassignmentRepository);
  }

  @Test
  void testRespond_Approved_UnassignsTheVehicle() {
    when(portalServiceClient.getUserById(ACCEPTOR_ID)).thenReturn(Mono.just(saeUser()));
    when(portalServiceClient.hasSaeRole(any())).thenReturn(true);
    when(unassignmentRepository.findById(50))
        .thenReturn(Mono.just(unassignment(50, 1, Constants.ID_STATUS_IN_REVISION, 20)),
            Mono.just(unassignment(50, 1, Constants.ID_STATUS_APPROVED, 20)));
    when(unassignmentRepository.updateStatusAndResponse(any(), any(), any()))
        .thenReturn(Mono.empty());
    when(unassignmentWorkflowRepository.saveWorkflow(any(), any(), any(), any()))
        .thenReturn(Mono.empty());
    when(vehicleRepository.unassignVehicle(1, Constants.ID_VEHICLE_STATUS_UNASSIGNED))
        .thenReturn(Mono.empty());
    mockDetailLookups(Constants.ID_STATUS_APPROVED, "APROBADO");

    StepVerifier.create(unassignmentService.respondToUnassignmentRequest(ACCEPTOR_ID, 50,
            new ParkingResponseIn().approved(true)))
        .assertNext(detail -> assertEquals("APROBADO", detail.getStatus()))
        .verifyComplete();

    Mockito.verify(vehicleRepository).unassignVehicle(1, Constants.ID_VEHICLE_STATUS_UNASSIGNED);
    Mockito.verify(vehicleRepository, Mockito.never()).deleteById(anyInt());
  }

  @Test
  void testRespond_Rejected_KeepsTheVehicleAssigned() {
    when(portalServiceClient.getUserById(ACCEPTOR_ID)).thenReturn(Mono.just(saeUser()));
    when(portalServiceClient.hasSaeRole(any())).thenReturn(true);
    when(unassignmentRepository.findById(50))
        .thenReturn(Mono.just(unassignment(50, 1, Constants.ID_STATUS_IN_REVISION, 20)),
            Mono.just(unassignment(50, 1, Constants.ID_STATUS_REJECTED, 20)));
    when(unassignmentRepository.updateStatusAndResponse(any(), any(), any()))
        .thenReturn(Mono.empty());
    when(unassignmentWorkflowRepository.saveWorkflow(any(), any(), any(), any()))
        .thenReturn(Mono.empty());
    mockDetailLookups(Constants.ID_STATUS_REJECTED, "RECHAZADO");

    StepVerifier.create(unassignmentService.respondToUnassignmentRequest(ACCEPTOR_ID, 50,
            new ParkingResponseIn().approved(false).comment("El vehículo tiene una multa pendiente.")))
        .assertNext(detail -> assertEquals("RECHAZADO", detail.getStatus()))
        .verifyComplete();

    Mockito.verify(vehicleRepository, Mockito.never()).unassignVehicle(anyInt(), anyInt());
  }

  @Test
  void testRespond_RejectedWithoutComment_IsInvalid() {
    StepVerifier.create(unassignmentService.respondToUnassignmentRequest(ACCEPTOR_ID, 50,
            new ParkingResponseIn().approved(false)))
        .expectErrorMatches(error -> error instanceof IllegalArgumentException
            && error.getMessage().equals(Constants.ERROR_COMMENT_REQUIRED_ON_REJECTION))
        .verify();

    Mockito.verifyNoInteractions(unassignmentRepository);
  }

  @Test
  void testRespond_WithoutApproved_IsInvalid() {
    StepVerifier.create(unassignmentService.respondToUnassignmentRequest(ACCEPTOR_ID, 50,
            new ParkingResponseIn()))
        .expectErrorMatches(error -> error instanceof IllegalArgumentException
            && error.getMessage().equals(Constants.ERROR_APPROVED_REQUIRED))
        .verify();

    Mockito.verifyNoInteractions(unassignmentRepository);
  }

  @Test
  void testRespond_ApplicantRoleIsForbidden() {
    when(portalServiceClient.getUserById(OWNER_ID)).thenReturn(Mono.just(studentUser()));
    when(portalServiceClient.hasSaeRole(any())).thenReturn(false);

    StepVerifier.create(unassignmentService.respondToUnassignmentRequest(OWNER_ID, 50,
            new ParkingResponseIn().approved(true)))
        .expectErrorMatches(error -> error instanceof ForbiddenException
            && error.getMessage().equals(Constants.ERROR_USER_NOT_SAE))
        .verify();

    Mockito.verifyNoInteractions(unassignmentRepository);
    Mockito.verify(vehicleRepository, Mockito.never()).unassignVehicle(anyInt(), anyInt());
  }

  @Test
  void testRespond_NotTheAssignedAcceptor_IsForbidden() {
    when(portalServiceClient.getUserById(ANOTHER_USER_ID)).thenReturn(Mono.just(saeUser()));
    when(portalServiceClient.hasSaeRole(any())).thenReturn(true);
    when(unassignmentRepository.findById(50))
        .thenReturn(Mono.just(unassignment(50, 1, Constants.ID_STATUS_IN_REVISION, 20)));

    StepVerifier.create(unassignmentService.respondToUnassignmentRequest(ANOTHER_USER_ID, 50,
            new ParkingResponseIn().approved(true)))
        .expectErrorMatches(error -> error instanceof ForbiddenException
            && error.getMessage().equals(Constants.ERROR_NOT_ACCEPTOR))
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).unassignVehicle(anyInt(), anyInt());
  }

  @Test
  void testRespond_AlreadyAnswered_IsConflict() {
    when(portalServiceClient.getUserById(ACCEPTOR_ID)).thenReturn(Mono.just(saeUser()));
    when(portalServiceClient.hasSaeRole(any())).thenReturn(true);
    when(unassignmentRepository.findById(50))
        .thenReturn(Mono.just(unassignment(50, 1, Constants.ID_STATUS_APPROVED, 20)));

    StepVerifier.create(unassignmentService.respondToUnassignmentRequest(ACCEPTOR_ID, 50,
            new ParkingResponseIn().approved(true)))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().equals(Constants.ERROR_UNASSIGNMENT_NOT_IN_REVIEW))
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).unassignVehicle(anyInt(), anyInt());
  }

  @Test
  void testRespond_UnknownRequest_IsNotFound() {
    when(portalServiceClient.getUserById(ACCEPTOR_ID)).thenReturn(Mono.just(saeUser()));
    when(portalServiceClient.hasSaeRole(any())).thenReturn(true);
    when(unassignmentRepository.findById(404)).thenReturn(Mono.empty());

    StepVerifier.create(unassignmentService.respondToUnassignmentRequest(ACCEPTOR_ID, 404,
            new ParkingResponseIn().approved(true)))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void testGetMyUnassignmentRequests_ReturnsTheApplicantRequests() {
    when(unassignmentRepository.findAllByIdApplicantOrderByIdUnassignmentRequestDesc(10))
        .thenReturn(Flux.just(unassignment(50, 1, Constants.ID_STATUS_REJECTED, 20)));
    mockDetailLookups(Constants.ID_STATUS_REJECTED, "RECHAZADO");

    StepVerifier.create(unassignmentService.getMyUnassignmentRequests(OWNER_ID))
        .assertNext(list -> {
          assertEquals(1, list.getUnassignmentRequests().size());
          assertEquals("RECHAZADO", list.getUnassignmentRequests().getFirst().getStatus());
          assertEquals("Juan", list.getUnassignmentRequests().getFirst().getApplicant()
              .getNameApplicant());
        })
        .verifyComplete();
  }

  @Test
  void testGetMyUnassignmentRequests_WithoutRequests_ReturnsEmptyList() {
    when(unassignmentRepository.findAllByIdApplicantOrderByIdUnassignmentRequestDesc(10))
        .thenReturn(Flux.empty());

    StepVerifier.create(unassignmentService.getMyUnassignmentRequests(OWNER_ID))
        .assertNext(list -> assertEquals(0, list.getUnassignmentRequests().size()))
        .verifyComplete();
  }

  @Test
  void testGetUnassignmentRequestsByAcceptor_AnotherAcceptor_IsForbidden() {
    StepVerifier.create(unassignmentService
            .getUnassignmentRequestsByAcceptor(ACCEPTOR_ID, ANOTHER_USER_ID.intValue()))
        .expectErrorMatches(error -> error instanceof ForbiddenException
            && error.getMessage().equals(Constants.ERROR_NOT_ACCEPTOR))
        .verify();

    Mockito.verifyNoInteractions(unassignmentRepository);
  }

  @Test
  void testGetUnassignmentRequestsByAcceptor_ReturnsAssignedRequests() {
    when(portalServiceClient.getUserById(ACCEPTOR_ID)).thenReturn(Mono.just(saeUser()));
    when(portalServiceClient.hasSaeRole(any())).thenReturn(true);
    when(unassignmentRepository.findAllByIdAcceptorOrderByIdUnassignmentRequestDesc(20))
        .thenReturn(Flux.just(unassignment(50, 1, Constants.ID_STATUS_IN_REVISION, 20)));
    mockDetailLookups(Constants.ID_STATUS_IN_REVISION, "EN_REVISION");

    StepVerifier.create(unassignmentService
            .getUnassignmentRequestsByAcceptor(ACCEPTOR_ID, ACCEPTOR_ID.intValue()))
        .assertNext(list -> {
          assertEquals(1, list.getUnassignmentRequests().size());
          assertEquals("Vendí el vehículo.",
              list.getUnassignmentRequests().getFirst().getReason());
        })
        .verifyComplete();
  }
}
