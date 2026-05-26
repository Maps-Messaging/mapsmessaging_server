/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.state.mavlink.packet;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * MAVLink SET_POSITION_TARGET_GLOBAL_INT mapped from typed outbound planning output.
 *
 * <p>The vertical reference is mandatory. This is intentional. The planner must
 * explicitly state whether the vertical value is relative to home, mean sea level,
 * terrain-relative, or another unsupported reference. Silent conversion is not
 * allowed here because it can produce unsafe navigation behaviour.</p>
 */
@Getter
public final class SetPositionTargetGlobalIntPacket extends OutboundMavlinkPacket {

  public static final String MESSAGE_NAME = "SET_POSITION_TARGET_GLOBAL_INT";

  public static final int IGNORE_VX = 1 << 3;
  public static final int IGNORE_VY = 1 << 4;
  public static final int IGNORE_VZ = 1 << 5;
  public static final int IGNORE_AFX = 1 << 6;
  public static final int IGNORE_AFY = 1 << 7;
  public static final int IGNORE_AFZ = 1 << 8;
  public static final int FORCE_SET = 1 << 9;
  public static final int IGNORE_YAW = 1 << 10;
  public static final int IGNORE_YAW_RATE = 1 << 11;

  public static final int POSITION_ONLY_TYPE_MASK =
      IGNORE_VX
          | IGNORE_VY
          | IGNORE_VZ
          | IGNORE_AFX
          | IGNORE_AFY
          | IGNORE_AFZ
          | IGNORE_YAW
          | IGNORE_YAW_RATE;

  private final long timeBootMilliseconds;
  private final int targetSystem;
  private final int targetComponent;
  private final MavlinkVerticalReference verticalReference;
  private final int typeMask;

  private final double latitude;
  private final double longitude;
  private final double verticalMeters;

  private final double velocityX;
  private final double velocityY;
  private final double velocityZ;

  private final double accelerationX;
  private final double accelerationY;
  private final double accelerationZ;

  private final double yaw;
  private final double yawRate;

  public SetPositionTargetGlobalIntPacket(
      int targetSystem,
      int targetComponent,
      double latitude,
      double longitude,
      double verticalMeters,
      MavlinkVerticalReference verticalReference
  ) {
    this(
        0L,
        targetSystem,
        targetComponent,
        verticalReference,
        POSITION_ONLY_TYPE_MASK,
        latitude,
        longitude,
        verticalMeters,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0
    );
  }

  public SetPositionTargetGlobalIntPacket(
      long timeBootMilliseconds,
      int targetSystem,
      int targetComponent,
      MavlinkVerticalReference verticalReference,
      int typeMask,
      double latitude,
      double longitude,
      double verticalMeters,
      double velocityX,
      double velocityY,
      double velocityZ,
      double accelerationX,
      double accelerationY,
      double accelerationZ,
      double yaw,
      double yawRate
  ) {
    validateTargetSystem(targetSystem);
    validateTargetComponent(targetComponent);
    validateLatitude(latitude);
    validateLongitude(longitude);
    validateVerticalReference(verticalReference);
    validateVerticalMeters(verticalMeters);
    validateFinite("velocityX", velocityX);
    validateFinite("velocityY", velocityY);
    validateFinite("velocityZ", velocityZ);
    validateFinite("accelerationX", accelerationX);
    validateFinite("accelerationY", accelerationY);
    validateFinite("accelerationZ", accelerationZ);
    validateFinite("yaw", yaw);
    validateFinite("yawRate", yawRate);

    this.timeBootMilliseconds = timeBootMilliseconds;
    this.targetSystem = targetSystem;
    this.targetComponent = targetComponent;
    this.verticalReference = verticalReference;
    this.typeMask = typeMask;
    this.latitude = latitude;
    this.longitude = longitude;
    this.verticalMeters = verticalMeters;
    this.velocityX = velocityX;
    this.velocityY = velocityY;
    this.velocityZ = velocityZ;
    this.accelerationX = accelerationX;
    this.accelerationY = accelerationY;
    this.accelerationZ = accelerationZ;
    this.yaw = yaw;
    this.yawRate = yawRate;
  }

  @Override
  public String getMessageName() {
    return MESSAGE_NAME;
  }

  public int getCoordinateFrame() {
    return verticalReference.getSetPositionTargetGlobalIntCoordinateFrame();
  }

  public int getLatitudeInt() {
    return (int) Math.round(latitude * 10_000_000.0);
  }

  public int getLongitudeInt() {
    return (int) Math.round(longitude * 10_000_000.0);
  }

  @Override
  public Map<String, Object> toFields() {
    Map<String, Object> fields = new LinkedHashMap<>();

    fields.put("time_boot_ms", timeBootMilliseconds);
    fields.put("target_system", targetSystem);
    fields.put("target_component", targetComponent);
    fields.put("coordinate_frame", getCoordinateFrame());
    fields.put("type_mask", typeMask);

    fields.put("lat_int", getLatitudeInt());
    fields.put("lon_int", getLongitudeInt());
    fields.put("alt", verticalMeters);

    fields.put("vx", velocityX);
    fields.put("vy", velocityY);
    fields.put("vz", velocityZ);

    fields.put("afx", accelerationX);
    fields.put("afy", accelerationY);
    fields.put("afz", accelerationZ);

    fields.put("yaw", yaw);
    fields.put("yaw_rate", yawRate);

    return fields;
  }

  private static void validateTargetSystem(int targetSystem) {
    if (targetSystem < 1 || targetSystem > 255) {
      throw new IllegalArgumentException("targetSystem must be between 1 and 255");
    }
  }

  private static void validateTargetComponent(int targetComponent) {
    if (targetComponent < 0 || targetComponent > 255) {
      throw new IllegalArgumentException("targetComponent must be between 0 and 255");
    }
  }

  private static void validateLatitude(double latitude) {
    if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
      throw new IllegalArgumentException("latitude must be between -90.0 and 90.0 degrees");
    }
  }

  private static void validateLongitude(double longitude) {
    if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
      throw new IllegalArgumentException("longitude must be between -180.0 and 180.0 degrees");
    }
  }

  private static void validateVerticalReference(MavlinkVerticalReference verticalReference) {
    if (verticalReference == null) {
      throw new IllegalArgumentException("verticalReference must be specified");
    }
    if (!verticalReference.isSupportedBySetPositionTargetGlobalInt()) {
      throw new IllegalArgumentException(
          "verticalReference " + verticalReference.name()
              + " is not supported by SET_POSITION_TARGET_GLOBAL_INT"
      );
    }
  }

  private static void validateVerticalMeters(double verticalMeters) {
    if (!Double.isFinite(verticalMeters)) {
      throw new IllegalArgumentException("verticalMeters must be finite");
    }
  }

  private static void validateFinite(String name, double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }
}