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

package io.mapsmessaging.dto.rest.config.network;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EndPointServerConfigDtoTest {

  @Test
  void getProtocolConfig_matchesExactTypeIgnoringCase() {
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    ProtocolConfigDTO mqtt = new MqttConfigDTO();
    config.setProtocolConfigs(List.of(new ProtocolConfigDTO("nats"), mqtt));

    assertSame(mqtt, config.getProtocolConfig("MQTT"));
  }

  @Test
  void getProtocolConfig_fallsBackToContainedType() {
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    ProtocolConfigDTO mqttVersion = new ProtocolConfigDTO("mqtt-v5");
    config.setProtocolConfigs(List.of(mqttVersion));

    assertSame(mqttVersion, config.getProtocolConfig("mqtt"));
  }

  @Test
  void getProtocolConfig_returnsNullWhenNoTypeMatches() {
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    config.setProtocolConfigs(List.of(new ProtocolConfigDTO("nats")));

    assertNull(config.getProtocolConfig("mqtt"));
  }

  @Test
  void getProtocols_usesMqttVersionWhenExplicit() {
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    MqttConfigDTO mqtt = new MqttConfigDTO();
    mqtt.setVersion(MqttVersion.MQTT_5);
    config.setProtocolConfigs(List.of(new ProtocolConfigDTO("nats"), mqtt));

    assertEquals("nats, MQTT_5", config.getProtocols());
  }

  @Test
  void getProtocols_usesMqttTypeWhenVersionIsAuto() {
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    config.setProtocolConfigs(List.of(new MqttConfigDTO()));

    assertEquals("mqtt", config.getProtocols());
  }
}
