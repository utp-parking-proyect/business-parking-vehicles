package com.utp.vehicles.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("status")
public class Status {

  @Id
  @Column("id_status")
  private Integer idStatus;

  @Column("name_status")
  private String nameStatus;
}
