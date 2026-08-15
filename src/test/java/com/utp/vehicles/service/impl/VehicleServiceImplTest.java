package com.utp.vehicles.service.impl;

import com.utp.vehicles.generated.model.VehicleIn;
import com.utp.vehicles.generated.model.VehicleStatus;
import com.utp.vehicles.mapper.VehicleInformationMapperImpl;
import com.utp.vehicles.model.entity.Vehicle;
import com.utp.vehicles.model.entity.VehicleType;
import com.utp.vehicles.repository.VehicleRepository;
import com.utp.vehicles.repository.VehicleTypeRepository;
import com.utp.vehicles.util.Constants;
import com.utp.vehicles.util.error.ConflictException;
import com.utp.vehicles.util.error.ForbiddenException;
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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

  private static final Long OWNER_ID = 10L;

  @Mock
  private VehicleRepository vehicleRepository;
  @Mock
  private VehicleTypeRepository vehicleTypeRepository;
  @Mock
  private TransactionalOperator transactionalOperator;

  @InjectMocks
  private VehicleServiceImpl vehicleService;

  @BeforeEach
  void mockTransactionalOperator() {
    Mockito.lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
  }

  @BeforeEach
  void useRealMapper() {
    ReflectionTestUtils.setField(vehicleService, "vehicleInformationMapper",
        new VehicleInformationMapperImpl());
  }

  private Vehicle assignedVehicle(Integer idVehicle, Integer idUser, String numberPlate) {
    Vehicle vehicle = new Vehicle();
    vehicle.setIdVehicle(idVehicle);
    vehicle.setIdUser(idUser);
    vehicle.setIdVehicleType(1);
    vehicle.setNumberPlate(numberPlate);
    vehicle.setIdVehicleStatus(Constants.ID_VEHICLE_STATUS_ASSIGNED);
    return vehicle;
  }

  private Vehicle unassignedVehicle(Integer idVehicle, String numberPlate) {
    Vehicle vehicle = new Vehicle();
    vehicle.setIdVehicle(idVehicle);
    vehicle.setIdUser(null);
    vehicle.setIdVehicleType(1);
    vehicle.setNumberPlate(numberPlate);
    vehicle.setIdVehicleStatus(Constants.ID_VEHICLE_STATUS_UNASSIGNED);
    return vehicle;
  }

  private VehicleType vehicleType() {
    VehicleType vehicleType = new VehicleType();
    vehicleType.setIdVehicleType(1);
    vehicleType.setNameVehicleType("Automóvil");
    return vehicleType;
  }

  private void mockAssignedVehiclesOfOwner(Vehicle... vehicles) {
    when(vehicleRepository.findAllByIdUserAndIdVehicleStatusOrderByIdVehicle(10,
        Constants.ID_VEHICLE_STATUS_ASSIGNED)).thenReturn(Flux.just(vehicles));
  }

  @Test
  void testGetMyVehicles_ReturnsOnlyAssignedVehiclesWithCounters() {
    mockAssignedVehiclesOfOwner(
        assignedVehicle(1, 10, "ABC-123"),
        assignedVehicle(2, 10, "XYZ-456"));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.just(vehicleType()));

    StepVerifier.create(vehicleService.getMyVehicles(OWNER_ID))
        .assertNext(list -> {
          assertEquals(2, list.getVehicles().size());
          assertEquals(2, list.getAssignedVehicles());
          assertEquals(Constants.MAX_ASSIGNED_VEHICLES_PER_USER, list.getMaxAssignedVehicles());
          assertEquals(VehicleStatus.ASSIGNED, list.getVehicles().getFirst().getStatus());
          assertEquals(VehicleStatus.ASSIGNED, list.getVehicles().getLast().getStatus());
          assertEquals("Automóvil", list.getVehicles().getFirst().getVehicleType());
        })
        .verifyComplete();
  }

  @Test
  void testGetMyVehicles_UnassignedVehiclesAreNotListed() {
    mockAssignedVehiclesOfOwner(assignedVehicle(1, 10, "ABC-123"));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.just(vehicleType()));

    StepVerifier.create(vehicleService.getMyVehicles(OWNER_ID))
        .assertNext(list -> {
          assertEquals(1, list.getVehicles().size());
          assertEquals("ABC-123", list.getVehicles().getFirst().getNumberPlate());
        })
        .verifyComplete();

    Mockito.verify(vehicleRepository).findAllByIdUserAndIdVehicleStatusOrderByIdVehicle(10,
        Constants.ID_VEHICLE_STATUS_ASSIGNED);
  }

  @Test
  void testGetMyVehicles_WithoutVehicles_ReturnsEmptyList() {
    when(vehicleRepository.findAllByIdUserAndIdVehicleStatusOrderByIdVehicle(10,
        Constants.ID_VEHICLE_STATUS_ASSIGNED)).thenReturn(Flux.empty());

    StepVerifier.create(vehicleService.getMyVehicles(OWNER_ID))
        .assertNext(list -> {
          assertTrue(list.getVehicles().isEmpty());
          assertEquals(0, list.getAssignedVehicles());
          assertEquals(Constants.MAX_ASSIGNED_VEHICLES_PER_USER, list.getMaxAssignedVehicles());
        })
        .verifyComplete();
  }

  @Test
  void testRegisterVehicle_FirstVehicle_IsCreatedAssigned() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("abc-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123")).thenReturn(Mono.empty());
    when(vehicleRepository.countByIdUserAndIdVehicleStatus(10,
        Constants.ID_VEHICLE_STATUS_ASSIGNED)).thenReturn(Mono.just(0L));
    when(vehicleRepository.insertVehicle(1, 10, "ABC-123",
        Constants.ID_VEHICLE_STATUS_ASSIGNED)).thenReturn(Mono.just(1));
    when(vehicleRepository.findById(1)).thenReturn(Mono.just(assignedVehicle(1, 10, "ABC-123")));
    when(vehicleTypeRepository.findById(1)).thenReturn(Mono.just(vehicleType()));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .assertNext(detail -> {
          assertEquals(1, detail.getIdVehicle());
          assertEquals("ABC-123", detail.getNumberPlate());
          assertEquals(VehicleStatus.ASSIGNED, detail.getStatus());
        })
        .verifyComplete();
  }

  @Test
  void testRegisterVehicle_SixthAssignedVehicle_IsConflict() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123")).thenReturn(Mono.empty());
    when(vehicleRepository.countByIdUserAndIdVehicleStatus(10,
        Constants.ID_VEHICLE_STATUS_ASSIGNED)).thenReturn(Mono.just(5L));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().equals(Constants.ERROR_MAX_ASSIGNED_VEHICLES_REACHED))
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).insertVehicle(anyInt(), anyInt(), any(), anyInt());
  }

  @Test
  void testRegisterVehicle_UnassignedVehiclesDoNotCountTowardsTheLimit() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123")).thenReturn(Mono.empty());
    when(vehicleRepository.countByIdUserAndIdVehicleStatus(10,
        Constants.ID_VEHICLE_STATUS_ASSIGNED)).thenReturn(Mono.just(4L));
    when(vehicleRepository.insertVehicle(1, 10, "ABC-123",
        Constants.ID_VEHICLE_STATUS_ASSIGNED)).thenReturn(Mono.just(6));
    when(vehicleRepository.findById(6)).thenReturn(Mono.just(assignedVehicle(6, 10, "ABC-123")));
    when(vehicleTypeRepository.findById(1)).thenReturn(Mono.just(vehicleType()));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .assertNext(detail -> assertEquals(VehicleStatus.ASSIGNED, detail.getStatus()))
        .verifyComplete();

    Mockito.verify(vehicleRepository).countByIdUserAndIdVehicleStatus(10,
        Constants.ID_VEHICLE_STATUS_ASSIGNED);
  }

  @Test
  void testRegisterVehicle_PlateAlreadyRegisteredByOwner_IsConflict() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123"))
        .thenReturn(Mono.just(assignedVehicle(1, 10, "ABC-123")));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().equals(Constants.ERROR_VEHICLE_ALREADY_REGISTERED))
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).insertVehicle(anyInt(), anyInt(), any(), anyInt());
  }

  @Test
  void testRegisterVehicle_PlateOfAnUnassignedVehicle_IsConflict() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123"))
        .thenReturn(Mono.just(unassignedVehicle(1, "ABC-123")));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectErrorMatches(error -> error instanceof ConflictException
            && error.getMessage().equals(Constants.ERROR_PLATE_REGISTERED_UNASSIGNED))
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).insertVehicle(anyInt(), anyInt(), any(), anyInt());
  }

  @Test
  void testRegisterVehicle_PlateOwnedByAnotherUser_IsForbidden() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(1);

    when(vehicleRepository.findByNumberPlate("ABC-123"))
        .thenReturn(Mono.just(assignedVehicle(1, 99, "ABC-123")));

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectError(ForbiddenException.class)
        .verify();

    Mockito.verify(vehicleRepository, Mockito.never()).insertVehicle(anyInt(), anyInt(), any(), anyInt());
  }

  @Test
  void testRegisterVehicle_MotorcycleWithCarPlate_IsInvalid() {
    VehicleIn vehicleIn = new VehicleIn().numberPlate("ABC-123").vehicleType(2);

    StepVerifier.create(vehicleService.registerVehicle(OWNER_ID, vehicleIn))
        .expectErrorMatches(error -> error instanceof IllegalArgumentException
            && error.getMessage().equals(Constants.ERROR_INVALID_NUMBER_PLATE_MOTORCYCLE))
        .verify();

    Mockito.verifyNoInteractions(vehicleRepository);
  }

  @Test
  void testGetMyVehicles_UnknownVehicleType_StillReturnsTheVehicle() {
    when(vehicleRepository.findAllByIdUserAndIdVehicleStatusOrderByIdVehicle(10,
        Constants.ID_VEHICLE_STATUS_ASSIGNED))
        .thenReturn(Flux.fromIterable(List.of(assignedVehicle(1, 10, "ABC-123"))));
    when(vehicleTypeRepository.findAllById(Set.of(1))).thenReturn(Flux.empty());

    StepVerifier.create(vehicleService.getMyVehicles(OWNER_ID))
        .assertNext(list -> assertEquals("ABC-123", list.getVehicles().getFirst().getNumberPlate()))
        .verifyComplete();
  }
}
