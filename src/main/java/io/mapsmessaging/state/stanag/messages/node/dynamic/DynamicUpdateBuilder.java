package io.mapsmessaging.state.stanag.messages.node.dynamic;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.Contact;
import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import io.mapsmessaging.state.stanag.messages.core.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.core.MessageType;
import io.mapsmessaging.state.stanag.messages.node.common.EntityDescription;
import io.mapsmessaging.state.stanag.messages.node.common.NodeMessageSupport;
import io.mapsmessaging.state.stanag.messages.node.common.Pose;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class DynamicUpdateBuilder {

  private static final String OPERATION_TYPE_PUT_VALUE =
      "DynamicUpdateOperationTypeEnum_PUT_VALUE";

  private static final String VALUE_TYPE_TRACK = "ValueTypeEnum_TRACK";

  private static final String CONTEXT_TYPE_SIMULATION = "ContextEnum_SIMULATION";

  private static final String STANDARD_IDENTITY_FRIEND = "StandardIdentityEnum_FRIEND";

  private static final String SYMBOL_SET_SEA_SURFACE = "SymbolSetEnum_SEA_SURFACE";

  private static final String ENTITY_STATUS_PRESENT = "EntityStatusEnum_PRESENT";

  private static final String ORGANIZATION_MAPS = "MAPS";

  private static final String NATIONALITY_EST = "EST";

  private static final String ENTITY_VERSION = "10";

  private static final String DEFAULT_ENTITY = "11";

  private static final String DEFAULT_ENTITY_TYPE = "11";

  private static final String DEFAULT_ENTITY_SUBTYPE = "00";

  private static final String DEFAULT_SECTOR_1 = "00";

  private static final String DEFAULT_SECTOR_2 = "00";

  private static final String HEADQUARTERS_TASK_FORCE_DUMMY_NOT_APPLICABLE =
      "HeadquartersTaskForceDummyEnum_NOT_APPLICABLE";

  private static final String UNIT_ECHELON_EQUIPMENT_MOBILITY_UNKNOWN =
      "UnitEchelonEquipmentMobilityEnum_UNKNOWN";

  private static final String TRACK_PHASE_TRACKED = "TrackPhase_TRACKED";

  private final MessageHeaderBuilder messageHeaderBuilder;

  private final NodeMessageSupport nodeMessageSupport;

  private final UUID sourceOfInformation;

  public Optional<DynamicUpdate> build(DroneTwin droneTwin, Contact contact) {
    Objects.requireNonNull(droneTwin, "droneTwin cannot be null");
    Objects.requireNonNull(contact, "contact cannot be null");
    Objects.requireNonNull(droneTwin.getUuid(), "droneTwin uuid cannot be null");

    if (contact.getId() == null || contact.getUpdatedTimeMs() <= 0) {
      return Optional.empty();
    }

    Pose pose = nodeMessageSupport.buildPose(contact.getPosition());

    if (pose == null) {
      return Optional.empty();
    }

    MessageHeader header = messageHeaderBuilder.build(
        MessageType.DYNAMIC_UPDATE,
        droneTwin.getUuid(),
        droneTwin.getLastSeenAt());

    Track track = Track.builder()
        .identifier(contact.getId())
        .description(buildEntityDescription(contact))
        .timestamp(Instant.ofEpochMilli(contact.getUpdatedTimeMs()))
        .pose(pose)
        .sourceOfInformation(sourceOfInformation)
        .timeOfInitiation(buildCreatedTimestamp(contact))
        .timeOfValidity(buildValidityTimestamp(contact))
        .trackPhase(TRACK_PHASE_TRACKED)
        .build();

    PutValue putValue = PutValue.builder()
        .discriminator(VALUE_TYPE_TRACK)
        .track(track)
        .build();

    DynamicUpdateOperation operation = DynamicUpdateOperation.builder()
        .discriminator(OPERATION_TYPE_PUT_VALUE)
        .putValue(putValue)
        .build();

    DynamicUpdateBody body = DynamicUpdateBody.builder()
        .operation(operation)
        .build();

    return Optional.of(new DynamicUpdate(header, body));
  }

  private EntityDescription buildEntityDescription(Contact contact) {
    if (contact.getDescription() == null || contact.getDescription().isBlank()) {
      return null;
    }

    return EntityDescription.builder()
        .version(ENTITY_VERSION)
        .name(contact.getDescription())
        .contextType(CONTEXT_TYPE_SIMULATION)
        .standardIdentity(STANDARD_IDENTITY_FRIEND)
        .symbolSet(SYMBOL_SET_SEA_SURFACE)
        .status(ENTITY_STATUS_PRESENT)
        .entity(DEFAULT_ENTITY)
        .entityType(DEFAULT_ENTITY_TYPE)
        .entitySubtype(DEFAULT_ENTITY_SUBTYPE)
        .sector1(DEFAULT_SECTOR_1)
        .sector2(DEFAULT_SECTOR_2)
        .headquartersTaskForceDummy(HEADQUARTERS_TASK_FORCE_DUMMY_NOT_APPLICABLE)
        .unitEchelonEquipmentMobility(UNIT_ECHELON_EQUIPMENT_MOBILITY_UNKNOWN)
        .organization(ORGANIZATION_MAPS)
        .nationality(NATIONALITY_EST)
        .build();
  }

  private Instant buildCreatedTimestamp(Contact contact) {
    if (contact.getCreatedTimeMs() <= 0) {
      return null;
    }

    return Instant.ofEpochMilli(contact.getCreatedTimeMs());
  }

  private Instant buildValidityTimestamp(Contact contact) {
    if (contact.getUpdatedTimeMs() <= 0 || contact.getTtlMillis() <= 0) {
      return null;
    }

    return Instant.ofEpochMilli(contact.getUpdatedTimeMs() + contact.getTtlMillis());
  }
}