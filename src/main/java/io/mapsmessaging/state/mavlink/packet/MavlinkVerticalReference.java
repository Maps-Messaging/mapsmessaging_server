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

import lombok.Getter;

/**
 * Vertical reference used by MAVLink planning output.
 *
 * <p>This must remain explicit. A target latitude and longitude without a clearly
 * defined vertical reference is unsafe because the same numeric value can mean
 * relative altitude, mean sea level altitude, terrain-relative altitude, or depth.</p>
 */
@Getter
public enum MavlinkVerticalReference {

  /**
   * Altitude above mean sea level.
   *
   * <p>Supported by SET_POSITION_TARGET_GLOBAL_INT using MAV_FRAME_GLOBAL_INT.</p>
   */
  MEAN_SEA_LEVEL(5, true),

  /**
   * Altitude relative to the vehicle home position.
   *
   * <p>Supported by SET_POSITION_TARGET_GLOBAL_INT using MAV_FRAME_GLOBAL_RELATIVE_ALT_INT.</p>
   */
  RELATIVE_TO_HOME(6, true),

  /**
   * Altitude above terrain.
   *
   * <p>Supported by SET_POSITION_TARGET_GLOBAL_INT using MAV_FRAME_GLOBAL_TERRAIN_ALT_INT.</p>
   */
  TERRAIN(11, true),

  /**
   * Height relative to the WGS84 ellipsoid.
   *
   * <p>Not supported by SET_POSITION_TARGET_GLOBAL_INT in this implementation.
   * The caller must convert this to a supported reference before encoding.</p>
   */
  WGS84_ELLIPSOID(-1, false),

  /**
   * Depth below the water surface.
   *
   * <p>Not supported by SET_POSITION_TARGET_GLOBAL_INT. This is intended for
   * marine or sub-surface planning models and must be mapped to an appropriate
   * vehicle-specific command before transmission.</p>
   */
  DEPTH_BELOW_SURFACE(-1, false);

  private final int coordinateFrame;
  private final boolean supportedBySetPositionTargetGlobalInt;

  MavlinkVerticalReference(
      int coordinateFrame,
      boolean supportedBySetPositionTargetGlobalInt
  ) {
    this.coordinateFrame = coordinateFrame;
    this.supportedBySetPositionTargetGlobalInt = supportedBySetPositionTargetGlobalInt;
  }

  public int getSetPositionTargetGlobalIntCoordinateFrame() {
    if (!supportedBySetPositionTargetGlobalInt) {
      throw new IllegalArgumentException(
          "Vertical reference " + name() + " cannot be encoded as SET_POSITION_TARGET_GLOBAL_INT"
      );
    }
    return coordinateFrame;
  }
}