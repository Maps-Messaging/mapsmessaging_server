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

package io.mapsmessaging.network.protocol.impl.semtech.status;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;

import java.net.SocketAddress;
import java.util.Objects;

public class SemtechStatusEventFactory {

  private static final SemtechStatusEventFactory INSTANCE = new SemtechStatusEventFactory();
  public static SemtechStatusEventFactory getInstance() {
    return INSTANCE;
  }


  private static final Gson GSON = new GsonBuilder().serializeNulls().create();

  // existing factory methods here ...

  public String toJson(SemtechStatusEvent event) {
    if (event == null) {
      throw new IllegalArgumentException("event must not be null");
    }
    return GSON.toJson(event);
  }

  public Message toMessage(SemtechStatusEvent event) {
    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(toJson(event).getBytes(java.nio.charset.StandardCharsets.UTF_8))
        .setContentType("application/json")
        .setQoS(QualityOfService.AT_MOST_ONCE);
    return builder.build();
  }


  public SemtechStatusEvent createGatewayEvent(String gatewayId, SemtechStatusState state) {
    validateGatewayState(state);
    return createBaseEvent(gatewayId, SemtechStatusType.GATEWAY, state);
  }

  public SemtechStatusEvent createDownlinkEvent(String gatewayId, SemtechStatusState state) {
    validateDownlinkState(state);
    return createBaseEvent(gatewayId, SemtechStatusType.DOWNLINK, state);
  }

  public SemtechStatusEvent createDownlinkEvent(
      String gatewayId,
      SemtechStatusState state,
      String messageId,
      Integer token,
      String error,
      String reason,
      Integer size,
      SocketAddress remoteAddress) {

    SemtechStatusEvent event = createDownlinkEvent(gatewayId, state);

    event.setMessageId(messageId);
    event.setToken(token);
    event.setError(error);
    event.setReason(reason);
    event.setSize(size);
    event.setRemoteAddress(toRemoteAddressString(remoteAddress));

    return event;
  }

  private SemtechStatusEvent createBaseEvent(String gatewayId, SemtechStatusType type, SemtechStatusState state) {
    Objects.requireNonNull(gatewayId, "gatewayId must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(state, "state must not be null");

    SemtechStatusEvent event = new SemtechStatusEvent();
    event.setGatewayId(gatewayId);
    event.setType(type);
    event.setState(state);
    event.setTimestampMillis(System.currentTimeMillis());
    return event;
  }

  private void validateGatewayState(SemtechStatusState state) {
    Objects.requireNonNull(state, "state must not be null");
    switch (state) {
      case GATEWAY_REGISTERED:
      case GATEWAY_PULL:
      case GATEWAY_EXPIRED:
      case GATEWAY_ADDRESS_CHANGED:
        return;

      default:
        throw new IllegalArgumentException("State is not a gateway state: " + state);
    }
  }

  private void validateDownlinkState(SemtechStatusState state) {
    Objects.requireNonNull(state, "state must not be null");
    switch (state) {
      case DOWNLINK_RECEIVED:
      case DOWNLINK_QUEUED:
      case DOWNLINK_SENT:
      case DOWNLINK_ACK_OK:
      case DOWNLINK_ACK_ERROR:
      case DOWNLINK_NO_ROUTE:
      case DOWNLINK_DROPPED:
        return;

      default:
        throw new IllegalArgumentException("State is not a downlink state: " + state);
    }
  }

  private String toRemoteAddressString(SocketAddress remoteAddress) {
    if (remoteAddress == null) {
      return null;
    }
    return remoteAddress.toString();
  }
}