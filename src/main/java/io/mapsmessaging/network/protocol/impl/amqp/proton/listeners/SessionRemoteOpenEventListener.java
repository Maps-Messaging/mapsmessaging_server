/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import java.io.IOException;
import javax.security.auth.login.LoginException;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Event.Type;
import org.apache.qpid.proton.engine.EventType;
import org.apache.qpid.proton.engine.Session;

public class SessionRemoteOpenEventListener extends BaseEventListener {

  public SessionRemoteOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Session ssn = event.getSession();
    if (ssn.getLocalState() == EndpointState.UNINITIALIZED) {
      try {
        io.mapsmessaging.network.protocol.impl.amqp.SessionManager sessionManager = createOrReuseSession(event.getConnection());
        ssn.setContext(sessionManager.getSession());
        ssn.open();
      } catch (LoginException | IOException e) {
        ssn.setCondition(new ErrorCondition(SESSION_ERROR, "Failed to establish session::" + e.getMessage()));
      }
    }
    return true;
  }

  @Override
  public EventType getType() {
    return Type.SESSION_REMOTE_OPEN;
  }
}
