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
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transformation pipeline tests for aggregator-2.
 *
 * Current intent:
 * - Publish messy JSON inputs on /aggregator2/in1, /in2, /in3.
 * - Aggregator outputs the standard envelope JSON (for now).
 * - Once inbound jsonMutate is configured, each input payload should be reduced to:
 *     in1: { "runId": "...", "temp": 20 }
 *     in2: { "runId": "...", "humidity": 60 }
 *     in3: { "runId": "...", "pressure": 990 }
 *
 * The test is expected to FAIL until transformers are configured. That is intentional.
 */
class StaticAggregatorTransformationSystemTest extends BaseAggreagtorTest {

  private static final long COMPLETE_PUBLISH_DEADLINE_MS = 500;
  private static final String JSON_CONTENT_TYPE = "application/json";

  private static final String OUT_TOPIC = "/aggregator2/out1";
  private static final String IN1 = "/aggregator2/in1";
  private static final String IN2 = "/aggregator2/in2";
  private static final String IN3 = "/aggregator2/in3";

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  private final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

  @Test
  void aggregator2_completePublishes_andInboundMutateProducesReducedFields() throws Exception {

    String runId = newRunId();
    BlockingQueue<Message> outQueue = new LinkedBlockingQueue<>();

    MessageListener listener = messageEvent -> onOutputMessage(outQueue, runId, messageEvent);

    Session session = createSession("agg2-transform-" + runId, listener);
    SubscribedEventManager outSub = addSubscription(OUT_TOPIC, session);

    try {
      drain(outQueue, 200);

      Message tempMessy = messyTemperature(runId, 20);
      Message humidityMessy = messyHumidity(runId, 60);
      Message pressureMessy = messyPressure(runId, 990);

      publish(IN1, tempMessy, session);
      publish(IN2, humidityMessy, session);

      publish(IN3, pressureMessy, session);

      Message out = pollOrFail(outQueue, COMPLETE_PUBLISH_DEADLINE_MS,
          "Expected aggregation output within " + COMPLETE_PUBLISH_DEADLINE_MS + "ms of third publish");

      assertNotNull(out.getOpaqueData(), "Output message had null payload");
      assertEquals(JSON_CONTENT_TYPE, out.getContentType(), "Output contentType must be application/json");

      String outJson = toUtf8(out.getOpaqueData());

      Map<String, Object> root = gson.fromJson(outJson, mapType);
      Object inputsObj = root.get("inputs");
      assertNotNull(inputsObj, "Envelope missing 'inputs'. Output=" + pretty(outJson));

      @SuppressWarnings("unchecked")
      Map<String, Object> inputs = (Map<String, Object>) inputsObj;

      assertEquals(3, inputs.size(), "Expected exactly 3 inputs present. Output=" + pretty(outJson));
      assertTrue(inputs.containsKey(IN1), "Missing input entry for " + IN1 + ". Output=" + pretty(outJson));
      assertTrue(inputs.containsKey(IN2), "Missing input entry for " + IN2 + ". Output=" + pretty(outJson));
      assertTrue(inputs.containsKey(IN3), "Missing input entry for " + IN3 + ". Output=" + pretty(outJson));

      // These are the transform assertions. They will FAIL until inbound jsonMutate is configured.
      assertEquals(20.0, numberPayloadField(inputs, IN1, "temp", outJson), "in1 missing/incorrect 'temp'. Output=" + pretty(outJson));
      assertEquals(60.0, numberPayloadField(inputs, IN2, "humidity", outJson), "in2 missing/incorrect 'humidity'. Output=" + pretty(outJson));
      assertEquals(990.0, numberPayloadField(inputs, IN3, "pressure", outJson), "in3 missing/incorrect 'pressure'. Output=" + pretty(outJson));

      // Keep runId as a stable filter signal until we decide to drop it later.
      assertEquals(runId, stringPayloadField(inputs, IN1, "runId", outJson), "in1 missing/incorrect 'runId'. Output=" + pretty(outJson));
      assertEquals(runId, stringPayloadField(inputs, IN2, "runId", outJson), "in2 missing/incorrect 'runId'. Output=" + pretty(outJson));
      assertEquals(runId, stringPayloadField(inputs, IN3, "runId", outJson), "in3 missing/incorrect 'runId'. Output=" + pretty(outJson));
    } finally {
      closeSubscription(outSub, session);
      closeSession(session);
    }
  }

  private void onOutputMessage(BlockingQueue<Message> outQueue, String runId, MessageEvent messageEvent) {
    try {
      Message message = messageEvent.getMessage();
      if (message == null) {
        return;
      }

      if (!OUT_TOPIC.equals(messageEvent.getDestinationName())) {
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

  private Message messyTemperature(String runId, int temperatureC) {
    String json =
        "{"
            + "\"runId\":\"" + runId + "\","
            + "\"deviceId\":\"dev-001\","
            + "\"deviceName\":\"temp-sensor\","
            + "\"time\":1700000000000,"
            + "\"location\":{\"lat\":-33.86,\"lon\":151.21},"
            + "\"battery\":{\"pct\":87},"
            + "\"reading\":{\"temperatureC\":" + temperatureC + "}"
            + "}";

    MessageBuilder builder = new MessageBuilder();
    builder.setContentType(JSON_CONTENT_TYPE);
    builder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8));
    return builder.build();
  }

  private Message messyHumidity(String runId, int humidityPct) {
    String json =
        "{"
            + "\"runId\":\"" + runId + "\","
            + "\"deviceId\":\"dev-002\","
            + "\"deviceName\":\"humidity-sensor\","
            + "\"time\":1700000000100,"
            + "\"location\":{\"lat\":-33.86,\"lon\":151.21},"
            + "\"signal\":{\"rssi\":-61},"
            + "\"reading\":{\"humidityPct\":" + humidityPct + "}"
            + "}";

    MessageBuilder builder = new MessageBuilder();
    builder.setContentType(JSON_CONTENT_TYPE);
    builder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8));
    return builder.build();
  }

  private Message messyPressure(String runId, int pressureHpa) {
    String json =
        "{"
            + "\"runId\":\"" + runId + "\","
            + "\"deviceId\":\"dev-003\","
            + "\"deviceName\":\"pressure-sensor\","
            + "\"time\":1700000000200,"
            + "\"location\":{\"lat\":-33.86,\"lon\":151.21},"
            + "\"calibration\":{\"offset\":2},"
            + "\"reading\":{\"pressureHpa\":" + pressureHpa + "}"
            + "}";

    MessageBuilder builder = new MessageBuilder();
    builder.setContentType(JSON_CONTENT_TYPE);
    builder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8));
    return builder.build();
  }

  private double numberPayloadField(Map<String, Object> inputs, String inputTopic, String fieldName, String fullOutputJson) {
    Map<String, Object> payload = payloadObject(inputs, inputTopic, fullOutputJson);

    Object value = payload.get(fieldName);
    assertNotNull(value, "Missing payload field '" + fieldName + "' for topic " + inputTopic + ". Output=" + pretty(fullOutputJson));

    if (value instanceof Number number) {
      return number.doubleValue();
    }
    fail("Payload field '" + fieldName + "' for topic " + inputTopic + " was not a number (was " + value.getClass().getName() + "). Output=" + pretty(fullOutputJson));
    return 0;
  }

  private String stringPayloadField(Map<String, Object> inputs, String inputTopic, String fieldName, String fullOutputJson) {
    Map<String, Object> payload = payloadObject(inputs, inputTopic, fullOutputJson);

    Object value = payload.get(fieldName);
    assertNotNull(value, "Missing payload field '" + fieldName + "' for topic " + inputTopic + ". Output=" + pretty(fullOutputJson));

    if (value instanceof String str) {
      return str;
    }
    fail("Payload field '" + fieldName + "' for topic " + inputTopic + " was not a string (was " + value.getClass().getName() + "). Output=" + pretty(fullOutputJson));
    return null;
  }

  private Map<String, Object> payloadObject(Map<String, Object> inputs, String inputTopic, String fullOutputJson) {
    Object entryObj = inputs.get(inputTopic);
    assertNotNull(entryObj, "Missing envelope entry for topic " + inputTopic + ". Output=" + pretty(fullOutputJson));

    @SuppressWarnings("unchecked")
    Map<String, Object> entry = (Map<String, Object>) entryObj;

    Object payloadObj = entry.get("payload");
    assertNotNull(payloadObj, "Expected 'payload' object for topic " + inputTopic + ". Output=" + pretty(fullOutputJson));

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadObj;

    return payload;
  }

  private String pretty(String json) {
    try {
      Object parsed = gson.fromJson(json, Object.class);
      return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(parsed);
    } catch (Exception e) {
      return json;
    }
  }

  private static String toUtf8(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static String newRunId() {
    return UUID.randomUUID().toString();
  }
}
