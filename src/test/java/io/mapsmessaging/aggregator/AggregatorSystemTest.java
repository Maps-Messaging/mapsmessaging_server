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

package io.mapsmessaging.aggregator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assumptions:
 * - The server is already running before tests execute and stays running.
 * - Aggregation output is JSON: {"inputs": { "<topic>": { "payload": {...} } } } for JSON inputs.
 * - Timeout starts at event0 (first contribution received for a new window).
 *
 * This test class uses runId embedded in JSON payload for filtering, so it remains stable even if
 * correlation propagation / dataMap propagation changes.
 */
class AggregatorSystemTest extends BaseAggreagtorTest {

  private static final long COMPLETE_PUBLISH_DEADLINE_MS = 500;
  private static final long TIMEOUT_MS = 5000;
  private static final long TIMEOUT_GRACE_MS = 500;
  private static final long NO_EARLY_PUBLISH_ASSERT_MS = 4500;

  private static final String JSON_CONTENT_TYPE = "application/json";

  private final Gson gson = new Gson();
  private final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

  @Test
  void aggregator1_completePublishesImmediately() throws Exception {
    AggregatorSpec spec = AggregatorSpec.aggregator1();
    runCompletePublishesImmediately(spec);
  }

  @Test
  void aggregator1_partialWaitsForTimeout() throws Exception {
    AggregatorSpec spec = AggregatorSpec.aggregator1();
    runPartialWaitsForTimeout(spec);
  }


  @Test
  void aggregator1_lastWinsWithinWindow() throws Exception {
    AggregatorSpec spec = AggregatorSpec.aggregator1();
    runLastWinsWithinWindow(spec);
  }


  private void runCompletePublishesImmediately(AggregatorSpec spec) throws LoginException, IOException, ExecutionException, InterruptedException, TimeoutException {

    String runId = newRunId();
    BlockingQueue<Message> outQueue = new LinkedBlockingQueue<>();

    MessageListener listener = messageEvent -> onMessage(outQueue, runId, messageEvent);

    Session session = createSession("agg-complete-" + runId, listener);
    SubscribedEventManager sub = addSubscription(spec.getOutputTopic(), session);

    try {
      drain(outQueue, 200);

      Message in1 = jsonMessage(runId, "topic", spec.getIn1(), "value", "v1");
      Message in2 = jsonMessage(runId, "topic", spec.getIn2(), "value", "v2");
      Message in3 = jsonMessage(runId, "topic", spec.getIn3(), "value", "v3");

      publish(spec.getIn1(), in1, session);
      publish(spec.getIn2(), in2, session);

      long thirdPublishTime = System.nanoTime();
      publish(spec.getIn3(), in3, session);

      Message out = pollOrFail(outQueue, COMPLETE_PUBLISH_DEADLINE_MS, "Expected output within " + COMPLETE_PUBLISH_DEADLINE_MS + "ms of third publish");
      long outDeltaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - thirdPublishTime);
      assertTrue(outDeltaMs <= COMPLETE_PUBLISH_DEADLINE_MS, "Output was not immediate; deltaMs=" + outDeltaMs);

      Envelope envelope = parseEnvelope(out);
      assertEquals(3, envelope.getInputs().size(), "Expected exactly 3 inputs in envelope");
      assertTrue(envelope.getInputs().containsKey(spec.getIn1()));
      assertTrue(envelope.getInputs().containsKey(spec.getIn2()));
      assertTrue(envelope.getInputs().containsKey(spec.getIn3()));

      assertEquals(runId, envelope.getPayloadField(spec.getIn1(), "runId"));
      assertEquals(runId, envelope.getPayloadField(spec.getIn2(), "runId"));
      assertEquals(runId, envelope.getPayloadField(spec.getIn3(), "runId"));

      assertEquals("v1", envelope.getPayloadField(spec.getIn1(), "value"));
      assertEquals("v2", envelope.getPayloadField(spec.getIn2(), "value"));
      assertEquals("v3", envelope.getPayloadField(spec.getIn3(), "value"));
    } finally {
      closeSubscription(sub, session);
      closeSession(session);
    }
  }

  private void runPartialWaitsForTimeout(AggregatorSpec spec)
      throws LoginException, IOException, ExecutionException, InterruptedException, TimeoutException {

    String runId = newRunId();
    BlockingQueue<Message> outQueue = new LinkedBlockingQueue<>();

    MessageListener listener = messageEvent -> onMessage(outQueue, runId, messageEvent);

    Session session = createSession("agg-timeout-" + runId, listener);
    SubscribedEventManager sub = addSubscription(spec.getOutputTopic(), session);

    try {
      drain(outQueue, 200);

      long t0 = System.nanoTime();
      Message in1 = jsonMessage(runId, "topic", spec.getIn1(), "value", "only1");
      publish(spec.getIn1(), in1, session);

      // Assert: no early publish well before timeout
      Message early = outQueue.poll(NO_EARLY_PUBLISH_ASSERT_MS, TimeUnit.MILLISECONDS);
      if (early != null) {
        fail("Received output early (before timeout). Envelope=" + toUtf8(early.getOpaqueData()));
      }

      long deadlineMs = TIMEOUT_MS + TIMEOUT_GRACE_MS;
      long remainingMs = deadlineMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
      if (remainingMs < 0) {
        remainingMs = 0;
      }

      Message out = pollOrFail(outQueue, remainingMs, "Expected output by timeout+grace (" + deadlineMs + "ms from event0)");
      long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
      assertTrue(elapsedMs <= deadlineMs, "Output missed timeout+grace; elapsedMs=" + elapsedMs);

      Envelope envelope = parseEnvelope(out);
      assertEquals(1, envelope.getInputs().size(), "Expected partial envelope with only 1 input present");
      assertTrue(envelope.getInputs().containsKey(spec.getIn1()), "Expected in1 present");
      assertFalse(envelope.getInputs().containsKey(spec.getIn2()), "Expected in2 missing");
      assertFalse(envelope.getInputs().containsKey(spec.getIn3()), "Expected in3 missing");

      assertEquals(runId, envelope.getPayloadField(spec.getIn1(), "runId"));
      assertEquals("only1", envelope.getPayloadField(spec.getIn1(), "value"));
    } finally {
      closeSubscription(sub, session);
      closeSession(session);
    }
  }

  private void runLastWinsWithinWindow(AggregatorSpec spec)
      throws LoginException, IOException, ExecutionException, InterruptedException, TimeoutException {

    String runId = newRunId();
    BlockingQueue<Message> outQueue = new LinkedBlockingQueue<>();

    MessageListener listener = messageEvent -> onMessage(outQueue, runId, messageEvent);

    Session session = createSession("agg-last-" + runId, listener);
    SubscribedEventManager sub = addSubscription(spec.getOutputTopic(), session);

    try {
      drain(outQueue, 200);

      Message first = jsonMessage(runId, "topic", spec.getIn1(), "value", "old");
      Message second = jsonMessage(runId, "topic", spec.getIn1(), "value", "new");
      Message in2 = jsonMessage(runId, "topic", spec.getIn2(), "value", "v2");
      Message in3 = jsonMessage(runId, "topic", spec.getIn3(), "value", "v3");

      publish(spec.getIn1(), first, session);
      publish(spec.getIn1(), second, session); // LAST should win
      publish(spec.getIn2(), in2, session);

      long thirdPublishTime = System.nanoTime();
      Thread.sleep(100);
      publish(spec.getIn3(), in3, session);

      Message out = pollOrFail(outQueue, COMPLETE_PUBLISH_DEADLINE_MS, "Expected output within " + COMPLETE_PUBLISH_DEADLINE_MS + "ms of third publish");
      long outDeltaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - thirdPublishTime);
      assertTrue(outDeltaMs <= COMPLETE_PUBLISH_DEADLINE_MS, "Output was not immediate; deltaMs=" + outDeltaMs);

      Envelope envelope = parseEnvelope(out);
      assertEquals("new", envelope.getPayloadField(spec.getIn1(), "value"), "LAST did not win for in1");
      assertEquals("v2", envelope.getPayloadField(spec.getIn2(), "value"));
      assertEquals("v3", envelope.getPayloadField(spec.getIn3(), "value"));
    } finally {
      closeSubscription(sub, session);
      closeSession(session);
    }
  }

  private void onMessage(BlockingQueue<Message> outQueue, String runId, MessageEvent messageEvent) {
    try {
      Message message = messageEvent.getMessage();
      if (message == null) {
        return;
      }

      if (!JSON_CONTENT_TYPE.equals(message.getContentType())) {
        return;
      }

      byte[] bytes = message.getOpaqueData();
      if (bytes == null) {
        return;
      }

      String json = toUtf8(bytes);
      if (!json.contains("\"runId\":\"" + runId + "\"")) {
        return;
      }

      outQueue.offer(message);
    } finally {
      Runnable completionTask = messageEvent.getCompletionTask();
      if (completionTask != null) {
        completionTask.run();
      }
    }
  }

  private void drain(BlockingQueue<Message> queue, long durationMs) throws InterruptedException {
    long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(durationMs);
    while (System.nanoTime() < end) {
      Message ignored = queue.poll(10, TimeUnit.MILLISECONDS);
      if (ignored == null) {
        continue;
      }
    }
  }

  private Message pollOrFail(BlockingQueue<Message> queue, long timeoutMs, String failureMessage) throws InterruptedException {
    Message message = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    if (message == null) {
      fail(failureMessage);
    }
    return message;
  }

  private Message jsonMessage(String runId, String key1, String value1, String key2, String value2) {
    String json = "{\"runId\":\"" + runId + "\",\"" + key1 + "\":\"" + value1 + "\",\"" + key2 + "\":\"" + value2 + "\"}";

    MessageBuilder builder = new MessageBuilder();
    builder.setContentType(JSON_CONTENT_TYPE);
    builder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8));
    return builder.build();
  }

  private Envelope parseEnvelope(Message outputMessage) {
    assertNotNull(outputMessage.getOpaqueData(), "Output message had null payload");
    String json = toUtf8(outputMessage.getOpaqueData());

    Map<String, Object> root = gson.fromJson(json, mapType);
    Object inputsObj = root.get("inputs");
    assertNotNull(inputsObj, "Envelope missing 'inputs'");

    @SuppressWarnings("unchecked")
    Map<String, Object> inputs = (Map<String, Object>) inputsObj;

    return new Envelope(inputs);
  }

  private static String toUtf8(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static String newRunId() {
    return UUID.randomUUID().toString();
  }
}
