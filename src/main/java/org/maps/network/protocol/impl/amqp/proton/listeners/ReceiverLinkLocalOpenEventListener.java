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

package org.maps.network.protocol.impl.amqp.proton.listeners;

import java.io.IOException;
import java.util.UUID;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.maps.messaging.api.features.DestinationType;
import org.maps.network.protocol.impl.amqp.AMQPProtocol;
import org.maps.network.protocol.impl.amqp.SessionManager;
import org.maps.network.protocol.impl.amqp.proton.ProtonEngine;

public class ReceiverLinkLocalOpenEventListener extends LinkLocalOpenEventListener {

  public ReceiverLinkLocalOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Link link = event.getLink();
    if(link instanceof Receiver){
      Receiver receiver = (Receiver) link;
      receiver.setSource(receiver.getRemoteSource());
      receiver.setTarget(receiver.getRemoteTarget());
      Target target = receiver.getTarget();
      if(target instanceof org.apache.qpid.proton.amqp.messaging.Target ) {
        org.apache.qpid.proton.amqp.messaging.Target messagingTarget = (org.apache.qpid.proton.amqp.messaging.Target) receiver.getTarget();
        if (messagingTarget.getDynamic()) {
          Symbol[] capabilities = messagingTarget.getCapabilities();
          DestinationType type = DestinationType.TEMPORARY_TOPIC;
          if (capabilities != null) {
            for (Symbol capability : capabilities) {
              if (capability.toString().contains("queue")) {
                type = DestinationType.TEMPORARY_QUEUE;
                break;
              }
            }
          }
          try {
            UUID uuid = UUID.randomUUID();
            String address = "/dynamic/" + type.getName() + "/" + uuid.toString();
            String sessionId = parseSessionId(event.getConnection().getRemoteContainer());
            SessionManager sessionManager = super.protocol.getSession(sessionId);
            sessionManager.getSession().findDestination(address, type);
            messagingTarget.setAddress(address);
          } catch (IOException e) {
            link.setCondition(new ErrorCondition(DYNAMIC_CREATION_ERROR, "Failed to create the dynamic destination::" + e.getMessage()));
          }
        }
      }
      receiver.open();
      return true;
    }
    return false;
  }

}
