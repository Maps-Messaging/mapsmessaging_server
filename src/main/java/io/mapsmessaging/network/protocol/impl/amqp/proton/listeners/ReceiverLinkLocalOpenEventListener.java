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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.security.uuid.UuidGenerator;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;

import java.io.IOException;
import java.util.UUID;

public class ReceiverLinkLocalOpenEventListener extends LinkLocalOpenEventListener {

  public ReceiverLinkLocalOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Link link = event.getLink();
    if (link instanceof Receiver) {
      Receiver receiver = (Receiver) link;
      try {
        prepareReceiver(event, receiver);
      } catch (IOException e) {
        link.setCondition(new ErrorCondition(DYNAMIC_CREATION_ERROR, "Failed to create the dynamic destination::" + e.getMessage()));
        receiver.open();
        receiver.close();
        return true;
      }
      receiver.open();
      return true;
    }
    return false;
  }

  static void prepareReceiver(Event event, Receiver receiver) throws IOException {
    receiver.setSource(receiver.getRemoteSource());
    Target remoteTarget = receiver.getRemoteTarget();
    receiver.setTarget(remoteTarget);
    if (remoteTarget instanceof org.apache.qpid.proton.amqp.messaging.Target messagingTarget) {
      handleDynamicTarget(event, messagingTarget);
    }
    retainDestinationName(receiver, remoteTarget);
  }

  private static void handleDynamicTarget(Event event, org.apache.qpid.proton.amqp.messaging.Target messagingTarget) throws IOException {
    if (messagingTarget.getDynamic() && (messagingTarget.getAddress() == null || messagingTarget.getAddress().isBlank())) {
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
      Object sessionContext = event.getSession().getContext();
      if (!(sessionContext instanceof io.mapsmessaging.api.Session session)) {
        throw new IOException("AMQP session is not established");
      }
      messagingTarget.setAddress(address);
      if (session.findDestination(address, type) == null) {
        throw new IOException("Destination manager returned no dynamic destination future");
      }
    }
  }

  private static boolean scanForQueue(org.apache.qpid.proton.amqp.messaging.Target messagingTarget) {
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
