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
 * Outbound transformation pipeline tests for aggregator-3.
 *
 * Intent:
 * - Publish already-clean JSON inputs on /aggregator3/in1, /in2, /in3.
 * - Aggregator builds its normal envelope internally, then outbound transformers reshape it.
 * - Expected final output is a flat JSON object containing:
 *     { "runId": "...", "temp": 20, "humidity": 60, "pressure": 990 }
 *
 * This test is expected to FAIL until aggregator-3 outbound transformers are configured.
 */
class StaticAggregatorOutboundTransformationSystemTest extends BaseAggreagtorTest {

  private static final long COMPLETE_PUBLISH_DEADLINE_MS = 500;
  private static final String JSON_CONTENT_TYPE = "application/json";

  private static final String OUT_TOPIC = "/aggregator3/out1";
  private static final String IN1 = "/aggregator3/in1";
  private static final String IN2 = "/aggregator3/in2";
  private static final String IN3 = "/aggregator3/in3";

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  private final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

  @Test
  void aggregator3_completePublishes_andOutboundTransformationFlattensEnvelope() throws Exception {

    String runId = newRunId();
    BlockingQueue<Message> outQueue = new LinkedBlockingQueue<>();

    MessageListener listener = messageEvent -> onOutputMessage(outQueue, runId, messageEvent);

    Session session = createSession("agg3-outbound-" + runId, listener);
    SubscribedEventManager outSub = addSubscription(OUT_TOPIC, session);

    try {
      drain(outQueue, 200);
      Message temp = cleanTemperature(runId, 20);
      Message humidity = cleanHumidity(runId, 60);
      Message pressure = cleanPressure(runId, 990);

      publish(IN3, pressure, session);
      publish(IN2, humidity, session);
      publish(IN1, temp, session);

      Message out = pollOrFail(outQueue, COMPLETE_PUBLISH_DEADLINE_MS,
          "Expected aggregation output within " + COMPLETE_PUBLISH_DEADLINE_MS + "ms of third publish");

      assertNotNull(out.getOpaqueData(), "Output message had null payload");
      assertEquals(JSON_CONTENT_TYPE, out.getContentType(), "Output contentType must be application/json");

      String outJson = toUtf8(out.getOpaqueData());
      Map<String, Object> root = gson.fromJson(outJson, mapType);

      // Critical outbound assertion: envelope must be flattened away.
      assertFalse(root.containsKey("inputs"), "Outbound output still contains 'inputs' (not flattened). Output=" + pretty(outJson));

      assertEquals(runId, stringField(root, "runId", outJson), "Missing/incorrect runId. Output=" + pretty(outJson));
      assertEquals(20.0, numberField(root, "temp", outJson), "Missing/incorrect temp. Output=" + pretty(outJson));
      assertEquals(60.0, numberField(root, "humidity", outJson), "Missing/incorrect humidity. Output=" + pretty(outJson));
      assertEquals(990.0, numberField(root, "pressure", outJson), "Missing/incorrect pressure. Output=" + pretty(outJson));
    } finally {
      closeSubscription(outSub, session);
      closeSession(session);
    }
  }

  private void onOutputMessage(BlockingQueue<Message> outQueue, String runId, MessageEvent messageEvent) {
    try {
      Message message = messageEvent.getMessage();
      System.out.println("Received message: " + messageEvent.getDestinationName());
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

      // Keep this filter: server is long-running and other tests exist.
      // Recommendation: keep runId in the outbound flattened output.
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

  private Message cleanTemperature(String runId, int temperatureC) {
    String json =
        "{"
            + "\"runId\":\"" + runId + "\","
            + "\"temp\":" + temperatureC
            + "}";

    MessageBuilder builder = new MessageBuilder();
    builder.setContentType(JSON_CONTENT_TYPE);
    builder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8));
    return builder.build();
  }

  private Message cleanHumidity(String runId, int humidityPct) {
    String json =
        "{"
            + "\"runId\":\"" + runId + "\","
            + "\"humidity\":" + humidityPct
            + "}";

    MessageBuilder builder = new MessageBuilder();
    builder.setContentType(JSON_CONTENT_TYPE);
    builder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8));
    return builder.build();
  }

  private Message cleanPressure(String runId, int pressureHpa) {
    String json =
        "{"
            + "\"runId\":\"" + runId + "\","
            + "\"pressure\":" + pressureHpa
            + "}";

    MessageBuilder builder = new MessageBuilder();
    builder.setContentType(JSON_CONTENT_TYPE);
    builder.setOpaqueData(json.getBytes(StandardCharsets.UTF_8));
    return builder.build();
  }

  private double numberField(Map<String, Object> root, String fieldName, String fullOutputJson) {
    Object value = root.get(fieldName);
    assertNotNull(value, "Missing field '" + fieldName + "'. Output=" + pretty(fullOutputJson));

    if (value instanceof Number number) {
      return number.doubleValue();
    }

    fail("Field '" + fieldName + "' was not a number (was " + value.getClass().getName() + "). Output=" + pretty(fullOutputJson));
    return 0;
  }

  private String stringField(Map<String, Object> root, String fieldName, String fullOutputJson) {
    Object value = root.get(fieldName);
    assertNotNull(value, "Missing field '" + fieldName + "'. Output=" + pretty(fullOutputJson));

    if (value instanceof String str) {
      return str;
    }

    fail("Field '" + fieldName + "' was not a string (was " + value.getClass().getName() + "). Output=" + pretty(fullOutputJson));
    return null;
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
