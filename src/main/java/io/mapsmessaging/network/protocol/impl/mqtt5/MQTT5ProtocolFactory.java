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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.MqttWillConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttVersion;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.ByteArrayDetection;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MessageProperties;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Publish5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ContentType;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageExpiryInterval;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.WillDelayInterval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MQTT5ProtocolFactory extends ProtocolImplFactory {

  private static final byte[] PROTOCOL;

  static {
    PROTOCOL = new byte[5];
    PROTOCOL[0] = 'M';
    PROTOCOL[1] = 'Q';
    PROTOCOL[2] = 'T';
    PROTOCOL[3] = 'T';
    PROTOCOL[4] = 5;
  }

  public MQTT5ProtocolFactory() {
    super("MQTT", "MQTT version 5.0 as per https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html", new ByteArrayDetection(PROTOCOL, 4, 2));
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    MQTT5Protocol protocol = new MQTT5Protocol(endPoint);
    EndPointServerConfigDTO dto = endPoint.getServer().getConfig();
    Publish5 willMsg = null;
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
        willMsg = new Publish5(
            payload,
            qos,
            0,
            will.getTopic(),
            will.isRetain()
            );
        MessageProperties properties = willMsg.getProperties();
        if(properties == null) {
          properties = new MessageProperties();
          willMsg.setProperties(properties);
        }
        if(will.getDelayInterval() > 0){
          WillDelayInterval willDelayInterval = new WillDelayInterval();
          willDelayInterval.setWillDelayInterval(will.getDelayInterval());
          properties.add(willDelayInterval);
        }
        if(will.getContentType() != null && !will.getContentType().isEmpty()){
          String contentType = will.getContentType();
          ContentType contentTypeObj = new ContentType(contentType);
          properties.add(contentTypeObj);
        }
        if(will.getMessageExpiryInterval() > 0){
          MessageExpiryInterval messageExpiryInterval = new MessageExpiryInterval(will.getMessageExpiryInterval());
          properties.add(messageExpiryInterval);
        }
      }
    }
    protocol.connect(sessionId, username, password, willMsg);
    return protocol;
  }

  @Override
  public boolean matches(String protocolName){
    return super.matches(protocolName) || MqttVersion.MQTT_5.name().equalsIgnoreCase(protocolName);
  }


  @Override
  public String getTransportType() {
    return "tcp";
  }


  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new MQTT5Protocol(endPoint, packet);
  }
}
