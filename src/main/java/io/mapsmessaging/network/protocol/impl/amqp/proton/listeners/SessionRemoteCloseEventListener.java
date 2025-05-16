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
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Event.Type;
import org.apache.qpid.proton.engine.EventType;
import org.apache.qpid.proton.engine.Session;

import java.io.IOException;

public class SessionRemoteCloseEventListener extends BaseEventListener {

  public SessionRemoteCloseEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Session ssn = event.getSession();
    if (ssn.getLocalState() != EndpointState.CLOSED) {
      io.mapsmessaging.api.Session session = (io.mapsmessaging.api.Session) ssn.getContext();
      if (session != null) {
        protocol.getLogger().log(ServerLogMessages.AMQP_CLOSED_SESSION, session.getName());
        try {
          protocol.delSession(session.getName());
        } catch (IOException e) {
          protocol.getLogger().log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
        }
        ssn.setContext(null); // remove from the context, to ensure no links
      }
      ssn.close();
      return true;
    }
    return false;
  }

  @Override
  public EventType getType() {
    return Type.SESSION_REMOTE_CLOSE;
  }
}
