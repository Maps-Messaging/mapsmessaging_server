/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.api.queue;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageAPITest;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.session.FakeProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import java.io.IOException;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    Destination queue = session.findDestination("aSimpleQueue", DestinationType.QUEUE);

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

    SessionManager.getInstance().close(session, false);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    messageEvent.getCompletionTask().run();
    messageEvent.getSubscription().ackReceived(messageEvent.getMessage().getIdentifier());
    System.err.println("Received event ::"+messageEvent.getMessage());
  }
}
