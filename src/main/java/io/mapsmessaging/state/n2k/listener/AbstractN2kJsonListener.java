/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
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

package io.mapsmessaging.state.n2k.listener;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;

import java.time.Instant;

public abstract class AbstractN2kJsonListener implements N2kJsonListener {

  protected Instant resolveTimestamp(TwinUpdateContext context) {
    if (context != null && context.getReceivedTime() != null) {
      return context.getReceivedTime();
    }
    return Instant.now();
  }

  protected boolean hasAny(JsonObject packet, String... names) {
    if (packet == null) {
      return false;
    }

    for (String name : names) {
      JsonElement jsonElement = packet.get(name);
      if (jsonElement != null && !jsonElement.isJsonNull()) {
        return true;
      }
    }

    return false;
  }

  protected Double getDouble(JsonObject packet, String... names) {
    JsonElement jsonElement = getElement(packet, names);
    if (jsonElement == null) {
      return null;
    }
    return jsonElement.getAsDouble();
  }

  protected Integer getInteger(JsonObject packet, String... names) {
    JsonElement jsonElement = getElement(packet, names);
    if (jsonElement == null) {
      return null;
    }
    return jsonElement.getAsInt();
  }

  protected Long getLong(JsonObject packet, String... names) {
    JsonElement jsonElement = getElement(packet, names);
    if (jsonElement == null) {
      return null;
    }
    return jsonElement.getAsLong();
  }

  protected String getString(JsonObject packet, String... names) {
    JsonElement jsonElement = getElement(packet, names);
    if (jsonElement == null) {
      return null;
    }
    return jsonElement.getAsString();
  }

  protected boolean isValidLatitude(Double latitude) {
    return latitude != null && latitude >= -90.0d && latitude <= 90.0d;
  }

  protected boolean isValidLongitude(Double longitude) {
    return longitude != null && longitude >= -180.0d && longitude <= 180.0d;
  }

  protected Double normalizeDegrees(Double degrees) {
    if (degrees == null) {
      return null;
    }

    double normalizedDegrees = degrees % 360.0d;
    if (normalizedDegrees < 0.0d) {
      normalizedDegrees += 360.0d;
    }

    return normalizedDegrees;
  }

  protected Double radiansToDegrees(Double radians) {
    if (radians == null) {
      return null;
    }
    return Math.toDegrees(radians);
  }

  private JsonElement getElement(JsonObject packet, String... names) {
    if (packet == null) {
      return null;
    }

    for (String name : names) {
      JsonElement jsonElement = packet.get(name);
      if (jsonElement != null && !jsonElement.isJsonNull()) {
        return jsonElement;
      }
    }

    return null;
  }
}