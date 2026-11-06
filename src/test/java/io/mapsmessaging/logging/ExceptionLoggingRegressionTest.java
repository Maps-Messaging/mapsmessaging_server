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

package io.mapsmessaging.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.mapsmessaging.aggregator.worker.AggregatorStripe;
import io.mapsmessaging.aggregator.worker.AggregatorStripeWorker;
import io.mapsmessaging.aggregator.worker.AggregatorWorkItem;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.auth.SubscriptionAuthorisationCheck;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.transformers.JSONToXML;
import io.mapsmessaging.api.transformers.JsonMapperTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.api.transformers.jsonmapper.JsonMapper;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.SubscribeListener;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Subscribe;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubscriptionInfo;
import io.mapsmessaging.security.access.Identity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExceptionLoggingRegressionTest {

  @Test
  void xml_conversion_failure_logs_throwable_and_preserves_payload() {
    try (LogCapture logs = new LogCapture(JSONToXML.class.getName())) {
      ParsedMessage input = invalidJson();
      byte[] before = input.getMessage().getOpaqueData();
      ParsedMessage result = new JSONToXML().transform("source", input);
      assertArrayEquals(before, result.getMessage().getOpaqueData());
      assertNotNull(logs.onlyEvent().getThrowableProxy());
    }
  }

  @Test
  void json_mapping_failure_logs_throwable_and_returns_original_message() {
    try (LogCapture logs = new LogCapture(JsonMapperTransformation.class.getName())) {
      ParsedMessage input = invalidJson();
      assertSame(input, new JsonMapperTransformation(new JsonMapper(List.of())).transform("source", input));
      assertNotNull(logs.onlyEvent().getThrowableProxy());
    }
  }

  @Test
  void authorisation_failure_logs_destination_and_remains_denied() {
    try (LogCapture logs = new LogCapture(SubscriptionAuthorisationCheck.class.getName())) {
      SubscriptionAuthorisationCheck check = new SubscriptionAuthorisationCheck(mock(Identity.class), null);
      assertFalse(check.check("/protected", DestinationType.TOPIC, false));
      ILoggingEvent event = logs.onlyEvent();
      assertTrue(event.getFormattedMessage().contains("/protected"));
      assertNotNull(event.getThrowableProxy());
    }
  }

  @Test
  void subscription_failure_logs_original_exception_and_returns_failure_suback() throws Exception {
    try (LogCapture logs = new LogCapture("MQTT_Packet_Listener")) {
      Session session = mock(Session.class);
      IOException failure = new IOException("subscription failed");
      doThrow(failure).when(session).addSubscription(any());
      Subscribe request = new Subscribe();
      request.setMessageId(17);
      request.getSubscriptionList().add(new SubscriptionInfo("/sensor", QualityOfService.getInstance(1)));
      SubAck response = assertInstanceOf(SubAck.class,
          new SubscribeListener().handlePacket(request, session, null, null));
      assertEquals(17, response.getPacketId());
      assertArrayEquals(new byte[] {(byte) 0x80}, response.getResult());
      ILoggingEvent event = logs.onlyEvent();
      assertTrue(event.getFormattedMessage().contains("/sensor"));
      assertSame(failure, assertInstanceOf(ThrowableProxy.class, event.getThrowableProxy()).getThrowable());
    }
  }

  @Test
  void aggregator_drain_failure_logs_original_exception_and_continues_to_next_item() {
    try (LogCapture logs = new LogCapture(AggregatorStripeWorker.class.getName())) {
      AggregatorStripe stripe = new AggregatorStripe();
      AggregatorStripeWorker worker = new AggregatorStripeWorker(stripe, 2, 0);
      IllegalStateException failure = new IllegalStateException("drain failed");
      stripe.signal(new TestWorkItem("failed-drain", () -> { throw failure; }, () -> {}));
      stripe.signal(new TestWorkItem("next-item", worker::shutdown, () -> {}));
      assertDoesNotThrow(worker::run);
      ILoggingEvent event = logs.onlyEvent();
      assertTrue(event.getFormattedMessage().contains("failed-drain"));
      assertSame(failure, assertInstanceOf(ThrowableProxy.class, event.getThrowableProxy()).getThrowable());
    }
  }

  @Test
  void aggregator_timeout_failure_logs_original_exception_and_continues_to_next_item() throws Exception {
    try (LogCapture logs = new LogCapture(AggregatorStripeWorker.class.getName())) {
      AggregatorStripe stripe = new AggregatorStripe();
      AggregatorStripeWorker worker = new AggregatorStripeWorker(stripe, 2, 0);
      IllegalStateException failure = new IllegalStateException("timeout failed");
      stripe.add(new TestWorkItem("failed-timeout", () -> {}, () -> { throw failure; }));
      stripe.add(new TestWorkItem("next-item", () -> {}, worker::shutdown));
      java.lang.reflect.Field lastTick = AggregatorStripeWorker.class.getDeclaredField("lastTimeoutTickMillis");
      lastTick.setAccessible(true);
      lastTick.setLong(worker, 0L);
      assertDoesNotThrow(worker::run);
      ILoggingEvent event = logs.onlyEvent();
      assertTrue(event.getFormattedMessage().contains("failed-timeout"));
      assertSame(failure, assertInstanceOf(ThrowableProxy.class, event.getThrowableProxy()).getThrowable());
    }
  }

  private static class TestWorkItem implements AggregatorWorkItem {
    private final String name;
    private final Runnable drain;
    private final Runnable timeout;

    TestWorkItem(String name, Runnable drain, Runnable timeout) {
      this.name = name;
      this.drain = drain;
      this.timeout = timeout;
    }

    @Override
    public String getName() { return name; }

    @Override
    public int drainOnce(int maxBatch) {
      drain.run();
      return 1;
    }

    @Override
    public void checkTimeout() { timeout.run(); }

    @Override
    public boolean tryMarkScheduled() { return true; }

    @Override
    public void clearScheduled() {}
  }

  private ParsedMessage invalidJson() {
    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData("{broken".getBytes(StandardCharsets.UTF_8));
    ParsedMessage result = new ParsedMessage();
    result.setMessage(builder.build());
    return result;
  }

  private static class LogCapture implements AutoCloseable {
    private final ch.qos.logback.classic.Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    LogCapture(String loggerName) {
      logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(loggerName);
      appender.start();
      logger.addAppender(appender);
    }

    ILoggingEvent onlyEvent() {
      assertEquals(1, appender.list.size(), "One failure log entry is expected: " + appender.list);
      return appender.list.getFirst();
    }

    @Override
    public void close() {
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}
