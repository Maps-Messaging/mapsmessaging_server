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

package io.mapsmessaging.api.overrides;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.*;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.session.FakeProtocol;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.test.WaitForState;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class BaseOverridesTest extends MessageAPITest implements MessageListener {

  private final AtomicLong counter = new AtomicLong();
  private Message lastMessage;

  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testRetainOverrideMemory(String type) throws Exception {
    testOverride("/messageOverrides/"+type+"/retain", msg ->
        Assertions.assertFalse(msg.isRetain()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testQoSOverrideMemory(String type) throws Exception {
    testOverride("/messageOverrides/"+type+"/qos", msg ->
        Assertions.assertEquals(QualityOfService.AT_MOST_ONCE, msg.getQualityOfService()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testExpiryOverride(String type) throws Exception {
    long expectedExpiry = System.currentTimeMillis() + 5000;
    testOverride("/messageOverrides/" + type + "/expiry", msg -> {
      long actualExpiry = msg.getExpiry();
      Assertions.assertTrue(Math.abs(actualExpiry - expectedExpiry) < 100,
          "Expected expiry near " + expectedExpiry + " but got " + actualExpiry);
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testMetaOverrideMemory(String type) throws Exception {
    testOverride("/messageOverrides/"+type+"/meta", msg ->
        Assertions.assertEquals("mval", msg.getMeta().get("mkey")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testDataMapOverrideMemory(String type) throws Exception {
    testOverride("/messageOverrides/"+type+"/datamap", msg ->
        Assertions.assertEquals("dval", msg.getDataMap().get("dkey").getData()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testStoreOfflineOverride(String type) throws Exception {
    testOverride("/messageOverrides/" + type + "/all", msg ->
        Assertions.assertTrue(msg.isStoreOffline()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testResponseTopicOverride(String type) throws Exception {
    testOverride("/messageOverrides/" + type + "/all", msg ->
        Assertions.assertEquals("/override/response", msg.getResponseTopic()));
  }
  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testPriorityOverride(String type) throws Exception {
    testOverride("/messageOverrides/" + type + "/all", msg ->
        Assertions.assertEquals(Priority.NORMAL, msg.getPriority()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"memory", "file"})
  void testAllOverridesMemory(String type) throws Exception {
    long expectedExpiry = System.currentTimeMillis() + 10000;
    testOverride("/messageOverrides/"+type+"/all", msg -> {
      long actualExpiry = msg.getExpiry();
      Assertions.assertTrue(Math.abs(actualExpiry - expectedExpiry) < 100,
          "Expected expiry near " + expectedExpiry + " but got " + actualExpiry);
      Assertions.assertEquals(QualityOfService.EXACTLY_ONCE, msg.getQualityOfService());
      Assertions.assertEquals("/override/response", msg.getResponseTopic());
      Assertions.assertEquals("application/xml", msg.getContentType());
      Assertions.assertEquals("test-schema", msg.getSchemaId());
      Assertions.assertEquals(Priority.NORMAL, msg.getPriority());
      Assertions.assertTrue(msg.isStoreOffline());
      Assertions.assertTrue(msg.isRetain());
      Assertions.assertEquals("mval", msg.getMeta().get("mkey"));
      Assertions.assertEquals("dval", msg.getDataMap().get("dkey").getData());
    });
  }

  private void testOverride(String namespace, java.util.function.Consumer<Message> validator) throws Exception {
    Session session = SessionManager.getInstance().create(
        new SessionContextBuilder("test_" + namespace, new ProtocolClientConnection(new FakeProtocol(this)))
            .setPersistentSession(true).build(), this);
    session.login();

    Destination destination = session.findDestination(namespace, DestinationType.TOPIC).get();
    SubscriptionContextBuilder subCtx = new SubscriptionContextBuilder(destination.getFullyQualifiedNamespace(), ClientAcknowledgement.AUTO)
        .setReceiveMaximum(1)
        .setCreditHandler(CreditHandler.AUTO)
        .setRetainHandler(RetainHandler.SEND_ALWAYS);
    session.addSubscription(subCtx.build());

    counter.set(0);
    this.lastMessage = null;

    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData("test".getBytes());
    messageBuilder.setStoreOffline(false);
    messageBuilder.setRetain(false);
    messageBuilder.setSchemaId("");
    messageBuilder.setResponseTopic("/initial");
    messageBuilder.setPriority(Priority.ONE_BELOW_NORMAL);
    messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
    destination.storeMessage(messageBuilder.build());

    WaitForState.waitFor(1, TimeUnit.SECONDS, () -> counter.get() > 0);
    Assertions.assertEquals(1, counter.get());
    Assertions.assertNotNull(lastMessage);
    validator.accept(lastMessage);

    SessionManager.getInstance().close(session, false);
  }


  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    lastMessage  = messageEvent.getMessage();
    counter.incrementAndGet();
    messageEvent.getCompletionTask().run();
  }
}
