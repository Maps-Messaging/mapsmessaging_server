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

package io.mapsmessaging.state.config.capability;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

@JsonAdapter(PlanTaskType.Adapter.class)
public enum PlanTaskType {

  REPOSITION,
  NAVIGATE,
  ESCORT,
  FOLLOW,
  IDENTIFY,
  TRACK,
  SHADOW,
  CLASSIFY,
  DETECT,
  SURVEY,
  PATROL,
  RECONNAISSANCE,
  PICKET,
  SCREEN,
  STANDBY,
  LOITER,
  PREPARE,
  SYNCHRONIZATION,
  HANDOVER,
  RESUPPLY,
  RECOVER,
  LAUNCH,
  DETER,
  WARN,
  COVER,
  MARK,
  DESTROY,
  NEUTRALIZE,
  ENGAGE,
  DEPLOY,
  REMOVE,
  JAM,
  RELAY,
  CLEAR,
  AVOID,
  BARRIER,
  INSPECT;

  private static final String WIRE_PREFIX = "PlanTaskType_";
  private static final String ENUM_PREFIX = "PlanTaskTypeEnum_";

  public static PlanTaskType fromConfigurationValue(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Plan task type is missing");
    }

    String normalized = normalize(value);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("Plan task type is missing after prefix");
    }

    try {
      return PlanTaskType.valueOf(normalized);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Unknown plan task type: " + value, exception);
    }
  }

  public String toWireValue() {
    return WIRE_PREFIX + name();
  }

  private static String normalize(String value) {
    if (value.startsWith(WIRE_PREFIX)) {
      return value.substring(WIRE_PREFIX.length());
    }
    if (value.startsWith(ENUM_PREFIX)) {
      return value.substring(ENUM_PREFIX.length());
    }
    return value;
  }

  public static final class Adapter extends TypeAdapter<PlanTaskType> {

    @Override
    public void write(JsonWriter out, PlanTaskType value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.toWireValue());
    }

    @Override
    public PlanTaskType read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      String value = in.nextString();
      try {
        return PlanTaskType.fromConfigurationValue(value);
      } catch (IllegalArgumentException exception) {
        throw new JsonParseException(exception.getMessage(), exception);
      }
    }
  }
}
