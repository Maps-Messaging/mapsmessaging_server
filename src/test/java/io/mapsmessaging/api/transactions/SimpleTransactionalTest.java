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

package io.mapsmessaging.api.transactions;

import io.mapsmessaging.api.MessageAPITest;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.engine.session.FakeProtocolImpl;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.security.auth.login.LoginException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.test.WaitForState;

class SimpleTransactionalTest extends MessageAPITest implements MessageListener {
  private static final int MESSAGE_COUNT = 10;

  private final AtomicLong counter = new AtomicLong(0);

  @Test
  void transactionalProcessing(TestInfo testInfo) throws LoginException, IOException {
    String name = "unknown";
    if(testInfo.getTestMethod().isPresent()){
      name = testInfo.getTestMethod().get().getName();
    }
    SessionContextBuilder scb = new SessionContextBuilder(name + "_1", new FakeProtocolImpl(this));
    scb.setReceiveMaximum(1); // ensure it is low
    scb.setSessionExpiry(2); // 2 seconds, more then enough time
    scb.setKeepAlive(120); // large enough to not worry about
    scb.setPersistentSession(true); // store the details

    Session session = SessionManager.getInstance().create(scb.build(), this);
    session.login();
    session.resumeState();
    Destination destination = session.findDestination("transaction", DestinationType.TOPIC);
    Assertions.assertNotNull(destination);
    Transaction transaction = session.startTransaction("testTransaction");
    Assertions.assertNotNull(transaction);
    counter.set(0);
    for(int x=0;x<MESSAGE_COUNT;x++) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setOpaqueData(("Hi There "+x).getBytes());
      messageBuilder.storeOffline(true);
      messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
      transaction.add(destination, messageBuilder.build());
    }
    // We should have 0 messages delivered

    Transaction transaction2 = session.startTransaction("testTransaction2");
    Assertions.assertNotNull(transaction2);
    counter.set(0);
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(("Hi There Single Event").getBytes());
    messageBuilder.storeOffline(true);
    messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
    transaction2.add(destination, messageBuilder.build());

    WaitForState.waitFor(1, TimeUnit.SECONDS, () -> counter.get() != 0);
    Assertions.assertEquals(0, counter.get());
    SubscriptionContextBuilder subscriptionContextBuilder = new SubscriptionContextBuilder(destination.getName(), ClientAcknowledgement.AUTO);
    subscriptionContextBuilder.setReceiveMaximum(MESSAGE_COUNT)
        .setCreditHandler(CreditHandler.AUTO)
        .setRetainHandler(RetainHandler.SEND_ALWAYS);
    Assertions.assertNotNull(session.addSubscription(subscriptionContextBuilder.build()));
    transaction.commit();

    WaitForState.waitFor(1, TimeUnit.SECONDS, () -> counter.get() == MESSAGE_COUNT);
    Assertions.assertEquals(MESSAGE_COUNT, counter.get());

    SessionManager.getInstance().close(session);

  }

  @Override
  public void sendMessage(@NotNull Destination destination,  @NotNull String normalisedName, @NotNull SubscribedEventManager subscription, @NotNull Message message,
      @NotNull Runnable completionTask) {
    counter.incrementAndGet();
    completionTask.run();
  }
}
