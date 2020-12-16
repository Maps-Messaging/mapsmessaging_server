/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.amqp.proton.listeners;

import java.io.IOException;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Event.Type;
import org.apache.qpid.proton.engine.EventType;
import org.apache.qpid.proton.engine.Session;
import org.maps.logging.LogMessages;
import org.maps.messaging.api.SessionManager;
import org.maps.network.protocol.impl.amqp.AMQPProtocol;
import org.maps.network.protocol.impl.amqp.proton.ProtonEngine;

public class SessionRemoteCloseEventListener extends BaseEventListener {

  public SessionRemoteCloseEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Session ssn = event.getSession();
    if (ssn.getLocalState() != EndpointState.CLOSED) {
      org.maps.messaging.api.Session session = (org.maps.messaging.api.Session)ssn.getContext();
      if(session != null){
        protocol.getLogger().log(LogMessages.AMQP_CLOSED_SESSION, session.getName());
        if(protocol.delSession(session.getName())) {
          try {
            SessionManager.getInstance().close(session);
          } catch (IOException e) {
            protocol.getLogger().log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
          }
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
