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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Event.Type;
import org.apache.qpid.proton.engine.EventType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConnectionRemoteOpenEventListener extends BaseEventListener {

  public ConnectionRemoteOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Connection conn = event.getConnection();
    if (conn.getLocalState() == EndpointState.UNINITIALIZED) {
      conn.open();
      Symbol[] symbols = conn.getRemoteDesiredCapabilities();
      List<Symbol> offered = getRemoteCapabilities(symbols);
      Symbol[] off = new Symbol[offered.size()];
      conn.setOfferedCapabilities(offered.toArray(off));
      Connection connection = (Connection) event.getContext();
      Map<Symbol, Object> remoteProperties = Optional.ofNullable(connection.getRemoteProperties())
          .orElse(new LinkedHashMap<>());
      protocol.setJMS(false);
      remoteProperties.keySet().forEach(symbol -> {
        Object val = remoteProperties.get(symbol);
        if (val != null) {
          String value = val.toString();
          if (value.toLowerCase().contains("qpidjms")) {
            protocol.setJMS(true);
            protocol.getLogger().log(ServerLogMessages.AMQP_DETECTED_JMS_CLIENT);
          }
          protocol.getLogger().log(ServerLogMessages.AMQP_REMOTE_CLIENT_PROPERTIES, symbol.toString(), val.toString());
        }
      });
      protocol.setSessionId(conn.getRemoteContainer());
    }
    return true;
  }

  @Override
  public EventType getType() {
    return Type.CONNECTION_REMOTE_OPEN;
  }
}
