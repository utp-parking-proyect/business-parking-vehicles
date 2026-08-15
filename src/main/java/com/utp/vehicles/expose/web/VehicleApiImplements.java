package com.utp.vehicles.expose.web;

import com.utp.vehicles.generated.api.VehiclesApi;
import com.utp.vehicles.generated.model.ParkingResponseIn;
import com.utp.vehicles.generated.model.VehicleAvailabilityIn;
import com.utp.vehicles.generated.model.VehicleDetail;
import com.utp.vehicles.generated.model.VehicleDetailList;
import com.utp.vehicles.generated.model.VehicleIn;
import com.utp.vehicles.generated.model.VehicleUnassignmentIn;
import com.utp.vehicles.generated.model.VehicleUnassignmentRequestDetail;
import com.utp.vehicles.generated.model.VehicleUnassignmentRequestList;
import com.utp.vehicles.service.VehicleService;
import com.utp.vehicles.service.VehicleUnassignmentService;
import com.utp.vehicles.util.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
@RequiredArgsConstructor
public class VehicleApiImplements implements VehiclesApi {

  private final VehicleService vehicleService;
  private final VehicleUnassignmentService vehicleUnassignmentService;
  private final AuthenticatedUserProvider authenticatedUserProvider;

  @Override
  public Mono<ResponseEntity<VehicleDetailList>> getMyVehicles(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      ServerWebExchange exchange) {

    return authenticatedUserProvider.getAuthenticatedUserId()
        .flatMap(vehicleService::getMyVehicles)
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<VehicleDetail>> registerVehicle(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Mono<VehicleIn> vehicleIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), vehicleIn)
        .flatMap(tuple -> vehicleService.registerVehicle(tuple.getT1(), tuple.getT2()))
        .map(vehicle -> {
          log.info("Vehicle registered successfully - VehicleId: {}", vehicle.getIdVehicle());
          return ResponseEntity.status(HttpStatus.CREATED)
              .header("Request-ID", requestID)
              .header("request-date", requestDate)
              .header("app-code", appCode)
              .header("caller-name", callerName)
              .body(vehicle);
        });
  }

  @Override
  public Mono<ResponseEntity<VehicleDetail>> updateVehicleAvailability(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer vehicleId,
      Mono<VehicleAvailabilityIn> vehicleAvailabilityIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), vehicleAvailabilityIn)
        .flatMap(tuple -> vehicleService.updateVehicleAvailability(tuple.getT1(), vehicleId, tuple.getT2()))
        .map(vehicle -> {
          log.info("Vehicle availability updated - VehicleId: {}, status: {}",
              vehicle.getIdVehicle(), vehicle.getStatus());
          return ResponseEntity.ok(vehicle);
        });
  }

  @Override
  public Mono<ResponseEntity<VehicleUnassignmentRequestDetail>> requestVehicleUnassignment(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer vehicleId,
      Mono<VehicleUnassignmentIn> vehicleUnassignmentIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), vehicleUnassignmentIn)
        .flatMap(tuple -> vehicleUnassignmentService
            .requestUnassignment(tuple.getT1(), vehicleId, tuple.getT2()))
        .map(unassignment -> {
          log.info("Vehicle unassignment requested - VehicleId: {}, UnassignmentId: {}",
              vehicleId, unassignment.getIdUnassignmentRequest());
          return ResponseEntity.status(HttpStatus.CREATED)
              .header("Request-ID", requestID)
              .header("request-date", requestDate)
              .header("app-code", appCode)
              .header("caller-name", callerName)
              .body(unassignment);
        });
  }

  @Override
  public Mono<ResponseEntity<VehicleUnassignmentRequestList>> getMyVehicleUnassignmentRequests(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      ServerWebExchange exchange) {

    return authenticatedUserProvider.getAuthenticatedUserId()
        .flatMap(vehicleUnassignmentService::getMyUnassignmentRequests)
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<VehicleUnassignmentRequestList>> getVehicleUnassignmentRequestsByAcceptor(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer acceptorId,
      ServerWebExchange exchange) {

    return authenticatedUserProvider.getAuthenticatedUserId()
        .flatMap(userId -> vehicleUnassignmentService
            .getUnassignmentRequestsByAcceptor(userId, acceptorId))
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<VehicleUnassignmentRequestDetail>> respondToVehicleUnassignmentRequest(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer unassignmentRequestId,
      Mono<ParkingResponseIn> parkingResponseIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), parkingResponseIn)
        .flatMap(tuple -> vehicleUnassignmentService
            .respondToUnassignmentRequest(tuple.getT1(), unassignmentRequestId, tuple.getT2()))
        .map(unassignment -> {
          log.info("Vehicle unassignment answered - UnassignmentId: {}, status: {}",
              unassignment.getIdUnassignmentRequest(), unassignment.getStatus());
          return ResponseEntity.ok(unassignment);
        });
  }
}
