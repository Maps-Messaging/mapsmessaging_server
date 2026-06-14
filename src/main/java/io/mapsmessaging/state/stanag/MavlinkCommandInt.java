package io.mapsmessaging.state.stanag;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.network.protocol.impl.mavlink.GsonFactory;
import io.mapsmessaging.state.drone.model.GeoPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MavlinkCommandInt {

  public static final int MESSAGE_ID_COMMAND_INT = 75;
  public static final int MAV_CMD_DO_REPOSITION = 192;
  public static final int MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6;

  private String messageType = "COMMAND_INT";
  private int messageId = MESSAGE_ID_COMMAND_INT;
  private int targetSystem;
  private int targetComponent;
  private int frame = MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
  private int command = MAV_CMD_DO_REPOSITION;
  private int current = 0;
  private int sequence;
  private int autocontinue = 0;
  private float param1 = -1.0f;
  private float param2 = 1.0f;
  private float param3 = 0.0f;
  private float param4 = 0.0f;
  private int latitude;
  private int longitude;
  private float altitude;

  private static final Gson gson = GsonFactory.createStrictJsonWithSafeFloats();

  public static MavlinkCommandInt reposition(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      float yawDegrees,
      int sequence) {
    MavlinkCommandInt commandInt = new MavlinkCommandInt();
    commandInt.setSequence(sequence);
    commandInt.setTargetSystem(targetSystem);
    commandInt.setTargetComponent(targetComponent);
    commandInt.setLatitude(toScaledCoordinate(position.getLatitude()));
    commandInt.setLongitude(toScaledCoordinate(position.getLongitude()));
    commandInt.setParam4(normaliseYawDegrees(yawDegrees));

    Double altitudeMeters = position.getPreferredAltitudeMeters();
    if (altitudeMeters != null) {
      commandInt.setAltitude(altitudeMeters.floatValue());
    }

    return commandInt;
  }

  public JsonObject toJsonObject() {
    return JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
  }

  public JsonObject toMavlinkJsonObject(int systemId, int componentId) {
    JsonObject root = new JsonObject();
    JsonObject header = new JsonObject();
    JsonObject payload = new JsonObject();

    header.addProperty("version", "V2");
    header.addProperty("systemId", systemId);
    header.addProperty("componentId", componentId);
    header.addProperty("sequence", sequence);
    header.addProperty("messageId", messageId);
    header.addProperty("signed", false);
    header.addProperty("incompatibilityFlags", 0);
    header.addProperty("compatibilityFlags", 0);

    payload.addProperty("target_system", targetSystem);
    payload.addProperty("target_component", targetComponent);
    payload.addProperty("frame", frame);
    payload.addProperty("command", command);
    payload.addProperty("current", current);
    payload.addProperty("autocontinue", autocontinue);
    payload.addProperty("param1", param1);
    payload.addProperty("param2", param2);
    payload.addProperty("param3", param3);
    payload.addProperty("param4", param4);
    payload.addProperty("x", latitude);
    payload.addProperty("y", longitude);
    payload.addProperty("z", altitude);

    root.add("header", header);
    root.add("payload", payload);

    return root;
  }

  private static int toScaledCoordinate(Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Coordinate must not be null");
    }
    return (int) Math.round(value * 10_000_000.0d);
  }

  private static float normaliseYawDegrees(float yawDegrees) {
    if (!Float.isFinite(yawDegrees)) {
      return 0.0f;
    }

    float normalisedYawDegrees = yawDegrees % 360.0f;
    if (normalisedYawDegrees < 0.0f) {
      normalisedYawDegrees += 360.0f;
    }

    return normalisedYawDegrees;
  }
}