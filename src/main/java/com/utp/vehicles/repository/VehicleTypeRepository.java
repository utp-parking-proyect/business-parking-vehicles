package com.utp.vehicles.repository;

import com.utp.vehicles.model.entity.VehicleType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleTypeRepository extends R2dbcRepository<VehicleType, Integer> {
}
