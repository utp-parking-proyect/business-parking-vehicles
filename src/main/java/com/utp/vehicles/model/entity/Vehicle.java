package com.utp.vehicles.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("vehicles")
public class Vehicle {

  @Id
  @Column("id_vehicle")
  private Integer idVehicle;

  @Column("id_vehicle_type")
  private Integer idVehicleType;

  @Column("id_user")
  private Integer idUser;

  @Column("number_plate")
  private String numberPlate;

  @Column("id_vehicle_status")
  private Integer idVehicleStatus;
}
