package com.utp.vehicles.service;

import com.utp.vehicles.generated.model.VehicleAvailabilityIn;
import com.utp.vehicles.generated.model.VehicleDetail;
import com.utp.vehicles.generated.model.VehicleDetailList;
import com.utp.vehicles.generated.model.VehicleIn;
import reactor.core.publisher.Mono;

public interface VehicleService {

  Mono<VehicleDetailList> getMyVehicles(Long authenticatedUserId);

  Mono<VehicleDetail> registerVehicle(Long authenticatedUserId, VehicleIn vehicle);

  Mono<VehicleDetail> updateVehicleAvailability(Long authenticatedUserId, Integer vehicleId,
      VehicleAvailabilityIn availability);
}
