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

package io.mapsmessaging.state.mavlink.bootstrap;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MavlinkBootstrapEvent {

  private String twinId;
  private int targetSystem;
  private int targetComponent;
  private MavlinkBootstrapEventType eventType;
  private MavlinkBootstrapRequestType requestType;
  private DroneTwinMissingState missingState;
  private DroneTwinReadinessState previousReadinessState;
  private DroneTwinReadinessState currentReadinessState;
  private int mavlinkMessageId;
  private int intervalMicroseconds;
  private String reason;
  private Instant createdAt;

  public static MavlinkBootstrapEvent requestMessage(
      String twinId,
      int targetSystem,
      int targetComponent,
      DroneTwinMissingState missingState,
      int mavlinkMessageId
  ) {
    MavlinkBootstrapEvent event = new MavlinkBootstrapEvent();
    event.setTwinId(twinId);
    event.setTargetSystem(targetSystem);
    event.setTargetComponent(targetComponent);
    event.setEventType(MavlinkBootstrapEventType.REQUEST);
    event.setRequestType(MavlinkBootstrapRequestType.REQUEST_MESSAGE);
    event.setMissingState(missingState);
    event.setMavlinkMessageId(mavlinkMessageId);
    event.setCreatedAt(Instant.now());
    return event;
  }

  public static MavlinkBootstrapEvent setMessageInterval(
      String twinId,
      int targetSystem,
      int targetComponent,
      DroneTwinMissingState missingState,
      int mavlinkMessageId,
      int intervalMicroseconds
  ) {
    MavlinkBootstrapEvent event = new MavlinkBootstrapEvent();
    event.setTwinId(twinId);
    event.setTargetSystem(targetSystem);
    event.setTargetComponent(targetComponent);
    event.setEventType(MavlinkBootstrapEventType.REQUEST);
    event.setRequestType(MavlinkBootstrapRequestType.SET_MESSAGE_INTERVAL);
    event.setMissingState(missingState);
    event.setMavlinkMessageId(mavlinkMessageId);
    event.setIntervalMicroseconds(intervalMicroseconds);
    event.setCreatedAt(Instant.now());
    return event;
  }

  public static MavlinkBootstrapEvent readinessChanged(
      String twinId,
      DroneTwinReadinessState previousReadinessState,
      DroneTwinReadinessState currentReadinessState
  ) {
    MavlinkBootstrapEvent event = new MavlinkBootstrapEvent();
    event.setTwinId(twinId);
    event.setEventType(MavlinkBootstrapEventType.READINESS_CHANGED);
    event.setPreviousReadinessState(previousReadinessState);
    event.setCurrentReadinessState(currentReadinessState);
    event.setCreatedAt(Instant.now());
    return event;
  }

  public static MavlinkBootstrapEvent completed(
      String twinId,
      DroneTwinReadinessState currentReadinessState
  ) {
    MavlinkBootstrapEvent event = new MavlinkBootstrapEvent();
    event.setTwinId(twinId);
    event.setEventType(MavlinkBootstrapEventType.BOOTSTRAP_COMPLETED);
    event.setCurrentReadinessState(currentReadinessState);
    event.setCreatedAt(Instant.now());
    return event;
  }

  public static MavlinkBootstrapEvent timedOut(
      String twinId,
      DroneTwinMissingState missingState,
      String reason
  ) {
    MavlinkBootstrapEvent event = new MavlinkBootstrapEvent();
    event.setTwinId(twinId);
    event.setEventType(MavlinkBootstrapEventType.BOOTSTRAP_TIMED_OUT);
    event.setMissingState(missingState);
    event.setReason(reason);
    event.setCreatedAt(Instant.now());
    return event;
  }
}