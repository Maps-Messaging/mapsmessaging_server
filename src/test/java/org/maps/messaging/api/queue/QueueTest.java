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

package org.maps.messaging.api.queue;

import java.io.IOException;
import javax.security.auth.login.LoginException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.MessageAPITest;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.MessageListener;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.messaging.api.SessionManager;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.SubscriptionContextBuilder;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.session.FakeProtocolImpl;
import org.maps.network.protocol.ProtocolImpl;

public class QueueTest extends MessageAPITest implements MessageListener {


  @Test
  void createQueueAndSendEvents() throws IOException, LoginException {
    ProtocolImpl protocol = new FakeProtocolImpl(this);
    SessionContextBuilder scb = new SessionContextBuilder("createQueueAndSendEvents_1", protocol);
    scb.setReceiveMaximum(1); // ensure it is low
    scb.setSessionExpiry(2); // 2 seconds, more then enough time
    scb.setKeepAlive(120); // large enough to not worry about
    scb.setPersistentSession(true); // store the details

    Session session = SessionManager.getInstance().create(scb.build(), protocol);
    Assertions.assertNotNull(session);
    session.resumeState();
    org.maps.messaging.api.Destination queue = session.findDestination("aSimpleQueue", DestinationType.QUEUE);

    Assertions.assertNotNull(queue);

    SubscriptionContextBuilder subContextBuilder = new SubscriptionContextBuilder("aSimpleQueue", ClientAcknowledgement.INDIVIDUAL);
    subContextBuilder.setReceiveMaximum(1000);
    subContextBuilder.setQos(QualityOfService.AT_LEAST_ONCE);
    SubscribedEventManager subscription = session.addSubscription(subContextBuilder.build());
    Assertions.assertNotNull(subscription);
    // OK we have a queue, so, apparently, we should be able to send events to it and they should be stored
    // this is different to a topic that simply drops them
    for (int x = 0; x < 10; x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE)
          .storeOffline(true)
          .setOpaqueData(new byte[1024]);
      queue.storeMessage(messageBuilder.build());
    }

    SessionManager.getInstance().close(session);
  }

  @Override
  public void sendMessage(@NotNull Destination destination, @NotNull String normalisedName, @NotNull SubscribedEventManager subscription,@NotNull Message message,@NotNull Runnable completionTask) {
    completionTask.run();
    subscription.ackReceived(message.getIdentifier());
    System.err.println("Received event ::"+message);
  }
}
