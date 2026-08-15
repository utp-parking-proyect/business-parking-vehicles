package com.utp.vehicles.repository;

import com.utp.vehicles.model.entity.VehicleUnassignmentWorkflow;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface VehicleUnassignmentWorkflowRepository
    extends R2dbcRepository<VehicleUnassignmentWorkflow, Integer> {

  @Query(value = """
      INSERT INTO vehicle_unassignment_workflow
        (id_unassignment_request, id_status, date_status_change, observation)
      VALUES (:unassignmentRequestId, :statusId, :dateStatusChange, :observation);
      """)
  Mono<Void> saveWorkflow(@Param("unassignmentRequestId") Integer unassignmentRequestId,
                          @Param("statusId") Integer statusId,
                          @Param("dateStatusChange") LocalDateTime dateStatusChange,
                          @Param("observation") String observation);

  @Query(value = """
      SELECT * FROM vehicle_unassignment_workflow
      WHERE id_unassignment_request = :unassignmentRequestId
      ORDER BY date_status_change, id_unassignment_workflow;
      """)
  Flux<VehicleUnassignmentWorkflow> findAllByUnassignmentRequestId(
      @Param("unassignmentRequestId") Integer unassignmentRequestId);
}
