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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.types.UTF8MessageProperty;

public class AuthenticationMethod extends UTF8MessageProperty {

  AuthenticationMethod() {
    super(MessagePropertyFactory.AUTHENTICATION_METHOD, "AuthenticationMethod");
  }

  public AuthenticationMethod(String method) {
    super(MessagePropertyFactory.AUTHENTICATION_METHOD, "AuthenticationMethod");
    value = method;
  }

  @Override
  public MessageProperty instance() {
    return new AuthenticationMethod();
  }

  public String getAuthenticationMethod() {
    return value;
  }

  public void setAuthenticationMethod(String authenticationMethod) {
    value = authenticationMethod;
  }
}
