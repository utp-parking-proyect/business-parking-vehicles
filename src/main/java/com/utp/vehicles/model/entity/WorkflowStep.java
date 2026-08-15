package com.utp.vehicles.model.entity;

import java.time.LocalDateTime;

public interface WorkflowStep {

  Integer getIdStatus();

  LocalDateTime getDateStatusChange();

  String getObservation();
}
