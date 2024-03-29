/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.api.features;

import lombok.Getter;

public enum DestinationMode {
  NORMAL(0, "Normal", "", "Normal Event publish/subscription", true),
  SCHEMA(1, "Schema", "$schema/", "Access to the destinations schema data", true),
  METRICS(2, "Metrics", "$metrics/", "Access to the destinations metrics", false);


  @Getter
  private final int id;
  @Getter
  private final String name;
  @Getter
  private final String description;
  @Getter
  private final boolean publishable;
  @Getter
  private final String namespace;

  private DestinationMode(int id, String name, String namespace, String description, boolean publishable) {
    this.id = id;
    this.name = name;
    this.namespace = namespace;
    this.description = description;
    this.publishable = publishable;
  }

  public static DestinationMode getInstance(int id) {
    switch (id) {
      case 0:
        return NORMAL;

      case 1:
        return SCHEMA;

      case 2:
        return METRICS;

      default:
        throw new IllegalArgumentException("Invalid handestination  mode value supplied:" + id);
    }
  }
}

