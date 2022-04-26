/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
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
      @NonNull @NotNull Connect connect) {
    super(
        factory,
        endPoint,
        remoteClient,
        selectorTask,
        "MQTT-SN 2.0 Protocol on " + endPoint.getName(),
        new PacketFactoryV2());
    stateEngine.setState(new InitialConnectionState());
    handleMQTTEvent(connect);
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  @Override
  public void sendKeepAlive() {
    writeFrame(new PingRequest());
    long timeout = System.currentTimeMillis() - (keepAlive + 1000);
    if (endPoint.getLastRead() < timeout && endPoint.getLastWrite() < timeout) {
      try {
        close();
      } catch (IOException e) {
        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
    }
  }

  @Override
  public String getName() {
    return "MQTT_SN_2.0";
  }

  @Override
  protected MQTT_SNPacket buildPublish(short alias, int packetId, MessageEvent messageEvent, QualityOfService qos){
    Publish publish = new Publish(alias, packetId,  messageEvent.getMessage().getOpaqueData());
    publish.setQoS(qos);
    publish.setCallback(messageEvent.getCompletionTask());
    return publish;
  }
}
