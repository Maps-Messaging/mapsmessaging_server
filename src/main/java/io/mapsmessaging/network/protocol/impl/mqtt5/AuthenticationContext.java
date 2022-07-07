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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.network.protocol.impl.SaslAuthenticationMechanism;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.security.sasl.Sasl;

public class AuthenticationContext {

  private final String authMethod;
  private final MQTTPacket5 parkedConnect;
  private final SaslAuthenticationMechanism mechanism;

  public AuthenticationContext(String authMethod, ConfigurationProperties properties, MQTTPacket5 parkedConnect) throws IOException {
    this.parkedConnect = parkedConnect;
    this.authMethod = authMethod;

    // Here we load the config into the props, where required
    Map<String, String> props = new HashMap<>();
    props.put(Sasl.QOP, "auth");
    mechanism = new SaslAuthenticationMechanism(authMethod, props);
  }

  public String getAuthMethod() {
    return authMethod;
  }

  public MQTTPacket5 getParkedConnect() {
    return parkedConnect;
  }

  public byte[] evaluateResponse(byte[] authenticationData) throws IOException {
    return mechanism.challenge(authenticationData);
  }

  public boolean isComplete() {
    return mechanism.complete();
  }
}
