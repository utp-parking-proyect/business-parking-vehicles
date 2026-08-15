package com.utp.vehicles.mapper;

import com.utp.vehicles.generated.client.users.model.CycleResponse;
import com.utp.vehicles.generated.client.users.model.UserResponse;
import com.utp.vehicles.generated.model.ApplicantInformation;
import com.utp.vehicles.generated.model.VehicleDetail;
import com.utp.vehicles.generated.model.VehicleInformation;
import com.utp.vehicles.generated.model.VehicleStatus;
import com.utp.vehicles.model.entity.Vehicle;
import com.utp.vehicles.model.entity.VehicleType;
import com.utp.vehicles.util.Constants;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VehicleInformationMapper {

  @Mapping(target = "numberPlate", source = "vehicle.numberPlate")
  @Mapping(target = "vehicleType", source = "vehicleType.nameVehicleType")
  VehicleInformation toVehicleInformation(Vehicle vehicle, VehicleType vehicleType);

  @Mapping(target = "idVehicle", source = "vehicle.idVehicle")
  @Mapping(target = "numberPlate", source = "vehicle.numberPlate")
  @Mapping(target = "idVehicleType", source = "vehicle.idVehicleType")
  @Mapping(target = "vehicleType", source = "vehicleType.nameVehicleType")
  @Mapping(target = "status", source = "vehicle.idVehicleStatus")
  VehicleDetail toVehicleDetail(Vehicle vehicle, VehicleType vehicleType);

  @Mapping(target = "idApplicant", source = "applicant.idUser")
  @Mapping(target = "nameApplicant", source = "applicant.name")
  @Mapping(target = "lastNameApplicant", source = "applicant.lastname")
  @Mapping(target = "usernameApplicant", source = "applicant.username")
  @Mapping(target = "numberCycle", source = "cycle.nameCycle")
  ApplicantInformation toApplicantInformation(UserResponse applicant, CycleResponse cycle);

  default VehicleStatus toVehicleStatus(Integer idVehicleStatus) {
    if (Constants.ID_VEHICLE_STATUS_ASSIGNED.equals(idVehicleStatus)) {
      return VehicleStatus.ASSIGNED;
    }
    return Constants.ID_VEHICLE_STATUS_UNASSIGNED.equals(idVehicleStatus)
        ? VehicleStatus.UNASSIGNED
        : null;
  }
}
