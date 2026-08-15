package com.utp.vehicles.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("vehicle_type")
public class VehicleType {

  @Id
  @Column("id_vehicle_type")
  private Integer idVehicleType;

  @Column("name_vehicle_type")
  private String nameVehicleType;
}
