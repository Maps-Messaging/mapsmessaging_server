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

package io.mapsmessaging.utilities.admin;

import lombok.Getter;
import lombok.ToString;

@ToString
public class HealthStatus {

  public enum LEVEL {INFO, WARN, ERROR, CRITICAL}

  @Getter
  private final String healthId;
  @Getter
  private final LEVEL level;
  @Getter
  private final String message;
  @Getter
  private final String resource;

  public HealthStatus(String healthId, LEVEL level, String message, String resource) {
    this.healthId = healthId;
    this.level = level;
    this.message = message;
    this.resource = resource;
  }
}
