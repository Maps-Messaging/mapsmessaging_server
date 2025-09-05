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

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.SessionManager;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.security.uuid.UuidGenerator;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReceiverLinkLocalOpenEventListener extends LinkLocalOpenEventListener {

  public ReceiverLinkLocalOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Link link = event.getLink();
    if (link instanceof Receiver) {
      Receiver receiver = (Receiver) link;
      receiver.setSource(receiver.getRemoteSource());
      receiver.setTarget(receiver.getRemoteTarget());
      Target target = receiver.getTarget();
      if (target instanceof org.apache.qpid.proton.amqp.messaging.Target) {
        org.apache.qpid.proton.amqp.messaging.Target messagingTarget = (org.apache.qpid.proton.amqp.messaging.Target) receiver.getTarget();
        handleDynamicTarget(event, link, messagingTarget);
      }
      receiver.open();
      return true;
    }
    return false;
  }

  private void handleDynamicTarget(Event event, Link link, org.apache.qpid.proton.amqp.messaging.Target messagingTarget) {
    if (messagingTarget.getDynamic()) {
      DestinationType type = DestinationType.TEMPORARY_TOPIC;
      UUID uuid = UuidGenerator.getInstance().generate();
      String address = "/dynamic/temporary/";
      if (scanForQueue(messagingTarget)) {
        type = DestinationType.TEMPORARY_QUEUE;
        address += "queue/";
      } else {
        address += "topic/";
      }
      address += uuid;
      String sessionId = parseSessionId(event.getConnection().getRemoteContainer());
      SessionManager sessionManager = super.protocol.getSession(sessionId);
      CompletableFuture<Destination> future = sessionManager.getSession().findDestination(address, type);
      if (future.isCompletedExceptionally()) {
        link.setCondition(new ErrorCondition(DYNAMIC_CREATION_ERROR, "Failed to create the dynamic destination::"));
      } else {
        messagingTarget.setAddress(address);
      }
    }
  }

  private boolean scanForQueue(org.apache.qpid.proton.amqp.messaging.Target messagingTarget) {
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
