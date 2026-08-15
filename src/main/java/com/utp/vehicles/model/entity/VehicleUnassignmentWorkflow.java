package com.utp.vehicles.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("vehicle_unassignment_workflow")
public class VehicleUnassignmentWorkflow implements WorkflowStep {

  @Id
  @Column("id_unassignment_workflow")
  private Integer idUnassignmentWorkflow;

  @Column("id_unassignment_request")
  private Integer idUnassignmentRequest;

  @Column("id_status")
  private Integer idStatus;

  @Column("date_status_change")
  private LocalDateTime dateStatusChange;

  @Column("observation")
  private String observation;
}
