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

package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechTransmitDefaultsDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
@Setter
public class GatewayInfo {
  private final SemtechTransmitDefaultsDTO transmitDefaults;
  private final Destination inbound;
  private final Destination status;
  private final Destination telemetry;

  private final SubscribedEventManager outbound;
  private final byte[] rawIdentifier;
  private final String name;
  private final Queue<MessageEvent> waitingMessages;

  private long lastAccess;

  public GatewayInfo(
      byte[] raw_identifier,
      String name,
      Destination inbound,
      Destination telemetry,
      Destination status,
      SubscribedEventManager outbound,
      SemtechTransmitDefaultsDTO transmitDefaults
  ) {
    this.rawIdentifier = raw_identifier;
    this.name = name;
    this.inbound = inbound;
    this.status = status;
    this.outbound = outbound;
    this.telemetry = telemetry;
    this.transmitDefaults = transmitDefaults;
    waitingMessages = new ConcurrentLinkedQueue<>();
    lastAccess = System.currentTimeMillis();
  }

  public void close(Session session) {
    session.removeSubscription(outbound.getContext().getAlias());
    waitingMessages.clear();
  }
}
