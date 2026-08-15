package com.utp.vehicles.util;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public final class Constants {

  public static final Integer ID_VEHICLE_TYPE_CAR = 1;
  public static final Integer ID_VEHICLE_TYPE_MOTORCYCLE = 2;
  public static final Integer ID_VEHICLE_TYPE_PICKUP = 3;

  public static final Set<Integer> ID_VEHICLE_TYPES_MOTORCYCLE = Set.of(ID_VEHICLE_TYPE_MOTORCYCLE);

  public static final String PATTERN_NUMBER_PLATE_CAR = "^[A-Z0-9]{3}-[A-Z0-9]{3}$";
  public static final String PATTERN_NUMBER_PLATE_MOTORCYCLE = "^[A-Z0-9]{2}-[A-Z0-9]{4}$";

  public static final Integer ID_STATUS_REGISTERED = 1;
  public static final Integer ID_STATUS_IN_REVISION = 2;
  public static final Integer ID_STATUS_APPROVED = 3;
  public static final Integer ID_STATUS_REJECTED = 4;

  public static final Integer ID_VEHICLE_STATUS_ACTIVE = 1;
  public static final Integer ID_VEHICLE_STATUS_DISABLED = 2;
  public static final Integer ID_VEHICLE_STATUS_UNASSIGNED = 3;

  public static final Set<Integer> ID_STATUSES_OPEN = Set.of(1, 2, 5);

  public static final Integer MAX_ACTIVE_VEHICLES_PER_USER = 5;

  public static final String ROLE_NAME_SAE = "ROLE_SAE";

  public static final String NAME_MICROSERVICE = "business-parking-request";

  public static final String OBSERVATION_IN_REVISION = "Solicitud asignada a Personal SAE para revisión.";
  public static final String OBSERVATION_UNASSIGNMENT_REGISTERED =
      "Solicitud de desasignación registrada.";
  public static final String OBSERVATION_UNASSIGNMENT_APPROVED_DEFAULT =
      "Desasignación aprobada. El vehículo dejó de estar asignado al solicitante.";
  public static final String OBSERVATION_UNASSIGNMENT_REJECTED_DEFAULT =
      "Desasignación rechazada. El vehículo continúa asignado al solicitante.";

  public static final String ERROR_VEHICLE_OWNED_BY_ANOTHER_USER =
      "El vehículo con esta placa pertenece a otro usuario";
  public static final String ERROR_VEHICLE_TYPE_REQUIRED =
      "El tipo de vehículo es requerido para registrar una placa nueva";
  public static final String ERROR_INVALID_NUMBER_PLATE_CAR =
      "La placa de un automóvil o camioneta debe tener el formato ABC-123 (3-3 caracteres)";
  public static final String ERROR_INVALID_NUMBER_PLATE_MOTORCYCLE =
      "La placa de una motocicleta debe tener el formato AB-1234 (2-4 caracteres)";
  public static final String ERROR_VEHICLE_ACTIVE_REQUIRED =
      "Debe indicar si el vehículo estará disponible para nuevas solicitudes";
  public static final String ERROR_VEHICLE_NOT_FOUND = "El vehículo no existe";
  public static final String ERROR_VEHICLE_NOT_OWNED =
      "No tienes permisos para modificar este vehículo";
  public static final String ERROR_VEHICLE_ALREADY_REGISTERED =
      "Ya tienes registrado un vehículo con esta placa";
  public static final String ERROR_MAX_ACTIVE_VEHICLES_REACHED =
      "Has alcanzado el máximo de " + MAX_ACTIVE_VEHICLES_PER_USER + " vehículos activos";
  public static final String ERROR_MAX_ACTIVE_VEHICLES_TO_ENABLE =
      "Ya tienes " + MAX_ACTIVE_VEHICLES_PER_USER
          + " vehículos activos. Deshabilita otro vehículo antes de habilitar este";
  public static final String ERROR_NOT_ACCEPTOR =
      "El usuario autenticado no es el aceptante consultado";
  public static final String ERROR_MISSING_USER_ID =
      "El token no contiene el identificador del usuario autenticado";
  public static final String ERROR_NO_ACCEPTOR_AVAILABLE = "No hay Personal SAE disponible para ser asignado";
  public static final String ERROR_UNASSIGNMENT_REASON_REQUIRED =
      "Debes indicar el motivo por el que solicitas desasignar el vehículo";
  public static final String ERROR_PLATE_REGISTERED_UNASSIGNED =
      "Esta placa ya está registrada y fue desasignada de su propietario anterior. "
          + "Comunícate con Personal SAE para reasignarla";
  public static final String ERROR_VEHICLE_ALREADY_UNASSIGNED =
      "El vehículo ya no está asignado a ningún usuario";
  public static final String ERROR_UNASSIGNMENT_ALREADY_IN_PROGRESS =
      "Ya existe una solicitud de desasignación en curso para este vehículo";
  public static final String ERROR_UNASSIGNMENT_NOT_FOUND =
      "La solicitud de desasignación no existe";
  public static final String ERROR_UNASSIGNMENT_NOT_IN_REVIEW =
      "La solicitud de desasignación no se encuentra en revisión y no puede ser respondida";
  public static final String ERROR_USER_NOT_SAE = "El usuario autenticado no es Personal SAE";
  public static final String ERROR_APPROVED_REQUIRED =
      "Debe indicar si la solicitud es aprobada o rechazada";
  public static final String ERROR_COMMENT_REQUIRED_ON_REJECTION =
      "Debe indicar el motivo del rechazo";
  public static final String ERROR_USERS_SERVICE_UNAVAILABLE = "business-core-portal no se encuentra disponible";
}
