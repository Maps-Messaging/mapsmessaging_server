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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.SessionManager;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import java.io.IOException;
import java.util.UUID;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;

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
        handleDynamicTarget(event, link, messagingTarget);
      }
      receiver.open();
      return true;
    }
    return false;
  }
  private void handleDynamicTarget(Event event, Link link, org.apache.qpid.proton.amqp.messaging.Target messagingTarget){
    if (messagingTarget.getDynamic()) {
      DestinationType type = DestinationType.TEMPORARY_TOPIC;
      UUID uuid = UUID.randomUUID();
      String address = "/dynamic/temporary/";
      if(scanForQueue(messagingTarget)){
        type = DestinationType.TEMPORARY_QUEUE;
        address+="queue/";
      }
      else{
        address +="topic/";
      }
      address+=uuid;
      try {
        String sessionId = parseSessionId(event.getConnection().getRemoteContainer());
        SessionManager sessionManager = super.protocol.getSession(sessionId);
        sessionManager.getSession().findDestination(address, type);
        messagingTarget.setAddress(address);
      } catch (IOException e) {
        link.setCondition(new ErrorCondition(DYNAMIC_CREATION_ERROR, "Failed to create the dynamic destination::" + e.getMessage()));
      }
    }
  }

  private boolean scanForQueue(org.apache.qpid.proton.amqp.messaging.Target messagingTarget){
    Symbol[] capabilities = messagingTarget.getCapabilities();
    if (capabilities != null) {
      for (Symbol capability : capabilities) {
        if (capability.toString().contains("queue")) {
          return true;
        }
      }
    }
    return false;
  }
}
