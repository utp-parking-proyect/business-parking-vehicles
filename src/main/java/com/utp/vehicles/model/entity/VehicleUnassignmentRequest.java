package com.utp.vehicles.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("vehicle_unassignment_requests")
public class VehicleUnassignmentRequest {

  @Id
  @Column("id_unassignment_request")
  private Integer idUnassignmentRequest;

  @Column("id_vehicle")
  private Integer idVehicle;

  @Column("id_applicant")
  private Integer idApplicant;

  @Column("id_acceptor")
  private Integer idAcceptor;

  @Column("id_status")
  private Integer idStatus;

  @Column("reason")
  private String reason;

  @Column("date_request")
  private LocalDateTime dateRequest;

  @Column("date_response")
  private LocalDateTime dateResponse;
}
