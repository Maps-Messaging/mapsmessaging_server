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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.MqttWillConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttVersion;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.MultiByteArrayDetection;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Publish;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MQTTProtocolFactory extends ProtocolImplFactory {

  private static final byte[][] PROTOCOL;

  static {
    PROTOCOL = new byte[][]{{'M', 'Q', 'T', 'T', 4}, {'M', 'Q', 'I', 's', 'd', 'p'}};
  }

  public MQTTProtocolFactory() {
    super("MQTT", "MQTT version 3.1.1 as per http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html", new MultiByteArrayDetection(PROTOCOL, 4, 2));
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    MQTTProtocol protocol = new MQTTProtocol(endPoint);
    EndPointServerConfigDTO dto = endPoint.getServer().getConfig();
    Publish willMsg = null;
    if(dto instanceof EndPointConnectionServerConfigDTO connectionServerConfigDTO) {
      if(connectionServerConfigDTO.getWillConfig() != null) {
        MqttWillConfigDTO will =  connectionServerConfigDTO.getWillConfig();
        QualityOfService qos = QualityOfService.getInstance(will.getQos());
        byte[] payload;
        if(will.getPayloadEncoding().equals("base64")) {
          payload = Base64.getDecoder().decode(will.getPayload());
        }
        else{
          payload = will.getPayload().getBytes(StandardCharsets.UTF_8);
        }
        willMsg = new Publish(
            will.isRetain(),
            payload,
            qos,
            0,
            will.getTopic()
        );
      }
    }
    protocol.connect(sessionId, username, password, willMsg);
    return protocol;
  }

  @Override
  public boolean matches(String protocolName){
    return super.matches(protocolName) || MqttVersion.MQTT_3_1_1.name().equalsIgnoreCase(protocolName);
  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new MQTTProtocol(endPoint, packet);
  }

  @Override
  public String getTransportType() {
    return "tcp";
  }

}
