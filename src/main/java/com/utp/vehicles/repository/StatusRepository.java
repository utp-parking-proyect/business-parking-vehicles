package com.utp.vehicles.repository;

import com.utp.vehicles.model.entity.Status;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusRepository extends R2dbcRepository<Status, Integer> {
}
