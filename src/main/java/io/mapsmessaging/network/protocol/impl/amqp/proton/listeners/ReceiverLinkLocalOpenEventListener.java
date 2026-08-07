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

import io.mapsmessaging.api.Destination;
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
      prepareAndOpen(event, receiver, engine);
      return true;
    }
    return false;
  }

  static void prepareAndOpen(Event event, Receiver receiver, ProtonEngine engine) {
    try {
      CompletableFuture<Destination> creation = prepareReceiver(event, receiver);
      if (creation == null) {
        receiver.open();
      } else {
        creation.whenComplete((destination, failure) -> engine.executeAndFlush(() -> completeDynamicOpen(receiver, destination, failure)));
      }
    } catch (IOException e) {
      failDynamicOpen(receiver, e.getMessage());
    }
  }

  static CompletableFuture<Destination> prepareReceiver(Event event, Receiver receiver) throws IOException {
    receiver.setSource(receiver.getRemoteSource());
    Target remoteTarget = receiver.getRemoteTarget();
    receiver.setTarget(remoteTarget);
    CompletableFuture<Destination> creation = null;
    if (remoteTarget instanceof org.apache.qpid.proton.amqp.messaging.Target messagingTarget) {
      creation = handleDynamicTarget(event, messagingTarget);
    }
    retainDestinationName(receiver, remoteTarget);
    return creation;
  }

  private static CompletableFuture<Destination> handleDynamicTarget(Event event, org.apache.qpid.proton.amqp.messaging.Target messagingTarget) throws IOException {
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
      CompletableFuture<Destination> creation = session.findDestination(address, type);
      if (creation == null) {
        throw new IOException("Destination manager returned no dynamic destination future");
      }
      return creation;
    }
    return null;
  }

  private static void completeDynamicOpen(Receiver receiver, Destination destination, Throwable failure) {
    if (failure != null) {
      failDynamicOpen(receiver, failure.getMessage());
    } else if (destination == null) {
      failDynamicOpen(receiver, "Destination manager returned no dynamic destination");
    } else {
      receiver.open();
    }
  }

  private static void failDynamicOpen(Receiver receiver, String reason) {
    receiver.setCondition(new ErrorCondition(DYNAMIC_CREATION_ERROR, "Failed to create the dynamic destination::" + reason));
    receiver.open();
    receiver.close();
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
