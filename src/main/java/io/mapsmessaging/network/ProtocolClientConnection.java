/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network;

import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.network.protocol.ProtocolImpl;

import java.security.Principal;

public class ProtocolClientConnection implements ClientConnection {

  private final ProtocolImpl protocol;
  public ProtocolClientConnection(ProtocolImpl protocol){
    this.protocol = protocol;
  }

  @Override
  public long getTimeOut() {
    return protocol.getTimeOut();
  }

  @Override
  public String getName() {
    return protocol.getName();
  }

  @Override
  public String getVersion() {
    return protocol.getVersion();
  }

  @Override
  public void sendKeepAlive() {
    protocol.sendKeepAlive();
  }

  @Override
  public Principal getPrincipal() {
    return protocol.getEndPoint().getEndPointPrincipal();
  }

  @Override
  public String getAuthenticationConfig() {
    return protocol.getEndPoint().getAuthenticationConfig();
  }

  @Override
  public String getUniqueName() {
    if (protocol.getEndPoint() != null) {
      return protocol.getEndPoint().getName();
    }
    return "";
  }
}
