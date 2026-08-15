package com.utp.vehicles.repository;

import com.utp.vehicles.model.entity.VehicleUnassignmentRequest;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface VehicleUnassignmentRequestRepository
    extends R2dbcRepository<VehicleUnassignmentRequest, Integer> {

  @Query(value = """
      INSERT INTO vehicle_unassignment_requests
        (id_vehicle, id_applicant, id_status, reason, date_request)
      VALUES (:idVehicle, :idApplicant, :idStatus, :reason, :dateRequest)
      RETURNING id_unassignment_request;
      """)
  Mono<Integer> insertUnassignmentRequest(@Param("idVehicle") Integer idVehicle,
                                          @Param("idApplicant") Integer idApplicant,
                                          @Param("idStatus") Integer idStatus,
                                          @Param("reason") String reason,
                                          @Param("dateRequest") LocalDateTime dateRequest);

  @Query(value = """
      UPDATE vehicle_unassignment_requests
      SET id_acceptor = :idAcceptor,
          id_status = :idStatus
      WHERE id_unassignment_request = :idUnassignmentRequest;
      """)
  Mono<Void> updateAcceptorAndStatus(@Param("idUnassignmentRequest") Integer idUnassignmentRequest,
                                     @Param("idAcceptor") Integer idAcceptor,
                                     @Param("idStatus") Integer idStatus);

  @Query(value = """
      UPDATE vehicle_unassignment_requests
      SET id_status = :idStatus,
          date_response = :dateResponse
      WHERE id_unassignment_request = :idUnassignmentRequest;
      """)
  Mono<Void> updateStatusAndResponse(@Param("idUnassignmentRequest") Integer idUnassignmentRequest,
                                     @Param("idStatus") Integer idStatus,
                                     @Param("dateResponse") LocalDateTime dateResponse);

  Flux<VehicleUnassignmentRequest> findAllByIdApplicantOrderByIdUnassignmentRequestDesc(
      Integer idApplicant);

  Flux<VehicleUnassignmentRequest> findAllByIdAcceptorOrderByIdUnassignmentRequestDesc(
      Integer idAcceptor);

  Mono<Long> countByIdAcceptorAndIdStatus(Integer idAcceptor, Integer idStatus);

  Mono<VehicleUnassignmentRequest> findFirstByIdVehicleAndIdStatusIn(Integer idVehicle,
                                                                     Collection<Integer> idStatuses);
}
