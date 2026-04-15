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

package io.mapsmessaging.state.drone.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Metadata about an incoming update.
 * Keeps update provenance separate from twin state itself.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwinUpdateContext {

  /** Logical updater/source, e.g. "mavlink-updater", "n2k-updater". */
  private String updateSource;

  /** Optional unique source instance/node id. */
  private String sourceInstanceId;

  /** Event timestamp from upstream payload if present. */
  private Instant eventTime;

  /** Server receive/processing time. */
  private Instant receivedTime;

  /** Optional monotonic sequence from source stream. */
  private Long sequenceNumber;

  /** Optional human-readable reason/label for tracing. */
  private String reason;

  /** True when update represents a full snapshot, false for partial patch. */
  private boolean fullSnapshot;
}
