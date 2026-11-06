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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;

import java.security.Principal;

/** Archive interest outlives the TCP connection and must never send its keepalives. */
public final class NatsArchiveConnection implements ClientConnection {

  private final String name;
  private final String version;
  private final String uniqueName;
  private final String protocolName;
  private final String remoteIp;
  private final String authenticationConfig;
  private final Principal principal;

  public NatsArchiveConnection(NatsProtocol protocol) {
    ProtocolClientConnection source = new ProtocolClientConnection(protocol);
    name = source.getName();
    version = source.getVersion();
    uniqueName = source.getUniqueName();
    protocolName = source.getProtocolName();
    remoteIp = source.getRemoteIp();
    authenticationConfig = source.getAuthenticationConfig();
    principal = source.getPrincipal();
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getUniqueName() {
    return uniqueName;
  }

  @Override
  public String getProtocolName() {
    return protocolName;
  }

  @Override
  public String getRemoteIp() {
    return remoteIp;
  }

  @Override
  public String getAuthenticationConfig() {
    return authenticationConfig;
  }

  @Override
  public Principal getPrincipal() {
    return principal;
  }

  @Override
  public void sendKeepAlive() {
    // Synchronous archive subscriptions have no network transport.
  }
}
