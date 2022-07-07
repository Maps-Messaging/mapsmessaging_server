/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.RegisteredTopicConfiguration;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.PacketFactoryV2;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.PingRequest;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.state.InitialConnectionState;
import java.io.IOException;
import java.net.SocketAddress;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

// The protocol is MQTT_SN, so it makes sense, ignoring the Camel Case rule in class names
@SuppressWarnings("squid:S00101")
public class MQTT_SNProtocolV2 extends MQTT_SNProtocol {

  public MQTT_SNProtocolV2(@NonNull @NotNull MQTTSNInterfaceManager factory,
      @NonNull @NotNull EndPoint endPoint,
      @NonNull @NotNull SocketAddress remoteClient,
      @NonNull @NotNull SelectorTask selectorTask,
      @NonNull @NotNull RegisteredTopicConfiguration registeredTopicConfiguration,
      @NonNull @NotNull Connect connect
  ) {
    super(
        factory,
        endPoint,
        remoteClient,
        selectorTask,
        "MQTT-SN 2.0 Protocol on " + endPoint.getName(),
        new PacketFactoryV2(),
        registeredTopicConfiguration);
    logger.log(ServerLogMessages.MQTT_SN_START, endPoint.getName());
    stateEngine.setState(new InitialConnectionState());
    addressKey = connect.getFromAddress();
    handleMQTTEvent(connect);
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      logger.log(ServerLogMessages.MQTT_SN_CLOSED);
      closed = true;
      finish();
    }
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  @Override
  public String getName() {
    return "MQTT_SN_2.0";
  }

  @Override
  public MQTT_SNPacket buildPublish(short alias, int packetId, MessageEvent messageEvent, QualityOfService qos, short topicTypeId) {
    byte[] data = messageEvent.getMessage().getOpaqueData();
    if (transformation != null) {
      data = transformation.outgoing(messageEvent.getMessage());
    }
    Publish publish = new Publish(alias, packetId, data);
    publish.setQoS(qos);
    publish.setTopicIdType(topicTypeId);
    publish.setCallback(messageEvent.getCompletionTask());
    return publish;
  }

  @Override
  public MQTT_SNPacket getPingRequest() {
    return new PingRequest();
  }
}
