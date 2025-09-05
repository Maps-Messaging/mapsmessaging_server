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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationMethod;
import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;
import lombok.Getter;
import lombok.Setter;

import javax.security.sasl.Sasl;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationContext {

  @Getter
  @Setter
  private AuthenticationMethod authenticationMethod;

  @Getter
  private final String authMethod;

  private final SaslAuthenticationMechanism mechanism;

  @Getter
  @Setter
  private MQTTPacket5 connectMsg;

  public AuthenticationContext(String authMethod, String serverName, String protocol, EndPointServerConfigDTO properties) throws IOException {
    this.authMethod = authMethod;
    // Here we load the config into the props, where required
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    mechanism = new SaslAuthenticationMechanism(authMethod, serverName, protocol, props, properties);
  }

  public byte[] evaluateResponse(byte[] authenticationData) throws IOException {
    return mechanism.challenge(authenticationData);
  }

  public boolean isComplete() {
    return mechanism.complete();
  }

  public String getUsername() {
    return mechanism.getUsername();
  }
}
