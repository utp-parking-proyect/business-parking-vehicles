package com.utp.vehicles.service;

import com.utp.vehicles.generated.model.ParkingResponseIn;
import com.utp.vehicles.generated.model.VehicleUnassignmentIn;
import com.utp.vehicles.generated.model.VehicleUnassignmentRequestDetail;
import com.utp.vehicles.generated.model.VehicleUnassignmentRequestList;
import reactor.core.publisher.Mono;

public interface VehicleUnassignmentService {

  Mono<VehicleUnassignmentRequestDetail> requestUnassignment(Long authenticatedUserId,
      Integer vehicleId, VehicleUnassignmentIn unassignment);

  Mono<VehicleUnassignmentRequestList> getMyUnassignmentRequests(Long authenticatedUserId);

  Mono<VehicleUnassignmentRequestList> getUnassignmentRequestsByAcceptor(Long authenticatedUserId,
      Integer acceptorId);

  Mono<VehicleUnassignmentRequestDetail> respondToUnassignmentRequest(Long authenticatedUserId,
      Integer unassignmentRequestId, ParkingResponseIn response);
}
