/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
package io.mapsmessaging.network.protocol.transformation.cloudevent.pack;

import io.mapsmessaging.schemas.config.SchemaConfig;
import org.jetbrains.annotations.NotNull;

public final class CloudEventTypeHelper {

  private CloudEventTypeHelper() { }

  public static @NotNull String deriveEventType(@NotNull SchemaConfig schemaConfig) {
    String base = "com.mapsmessaging";

    String entity = firstNonEmpty(
        schemaConfig.getResourceType(),
        schemaConfig.getInterfaceDescription(),
        schemaConfig.getFormat(),
        "event"
    );

    String version = schemaConfig.getVersion() != null && !schemaConfig.getVersion().isEmpty() ? ".v" + schemaConfig.getVersion() : "";

    return base + "." + sanitize(entity) + version;
  }

  private static String firstNonEmpty(String... values) {
    for (String v : values) {
      if (v != null && !v.isEmpty()) return v;
    }
    return null;
  }

  private static String sanitize(String s) {
    return s.toLowerCase().replaceAll("[^a-z0-9]+", ".");
  }
}
