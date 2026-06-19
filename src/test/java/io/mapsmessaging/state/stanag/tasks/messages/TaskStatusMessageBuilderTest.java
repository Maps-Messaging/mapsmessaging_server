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

package io.mapsmessaging.state.stanag.tasks.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import io.mapsmessaging.state.GsonStanagHelper;

import io.mapsmessaging.state.stanag.messages.*;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackMessage;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackMessageBuilder;
import io.mapsmessaging.state.stanag.messages.result.ResultReason;
import io.mapsmessaging.state.stanag.messages.result.ResultReasonBuilder;
import io.mapsmessaging.state.stanag.messages.result.TaskResultMessage;
import io.mapsmessaging.state.stanag.messages.result.TaskResultMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskStatusMessageBuilderTest {

  private static final UUID TASK_IDENTIFIER = UUID.fromString("7f1f8d0b-91bc-4f89-ae4e-5cb2e6d9a6a8");
  private static final UUID NODE_IDENTIFIER = UUID.fromString("53a15a3e-b8da-5dbe-bb9d-fdf3a8ff8159");
  private static final Instant FIXED_TIME = Instant.parse("2026-06-05T06:16:05.021Z");

  private Gson gson;
  private TaskStatusContext context;
  private TaskFeedbackMessageBuilder feedbackMessageBuilder;
  private TaskResultMessageBuilder resultMessageBuilder;

  @BeforeEach
  void setUp() {
    gson = GsonStanagHelper.createGson();

    Clock clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
    MessageHeaderBuilder headerBuilder = new MessageHeaderBuilder(clock);

    feedbackMessageBuilder = new TaskFeedbackMessageBuilder(headerBuilder);
    resultMessageBuilder = new TaskResultMessageBuilder(headerBuilder, new ResultReasonBuilder());

    context = new TaskStatusContext(TASK_IDENTIFIER, NODE_IDENTIFIER, null);
  }

  @Test
  void shouldBuildAcceptedFeedbackMessage() {
    TaskFeedbackMessage message = feedbackMessageBuilder.buildAccepted(context);

    JsonObject json = toJsonObject(message);
    JsonObject header = json.getAsJsonObject("header");
    JsonObject body = json.getAsJsonObject("body");

    assertEquals("MessageTypeEnum_TASK_FEEDBACK", header.get("message_type").getAsString());
    assertEquals(NODE_IDENTIFIER, header.get("source").getAsString());
    assertEquals("2026-06-05T06:16:05.021Z", header.get("time_sent").getAsString());
    assertEquals("0.3.0", header.get("version").getAsString());

    assertEquals(TASK_IDENTIFIER, body.get("identifier").getAsString());
    assertEquals(NODE_IDENTIFIER, body.get("node").getAsString());
    assertEquals("TaskStateEnum_ACTIVE", body.get("state").getAsString());

    assertFalse(body.has("percent_complete"));
    assertFalse(body.has("time_remaining"));
    assertFalse(body.has("waypoints_remaining"));
  }

  @Test
  void shouldBuildProgressFeedbackMessage() {
    TaskFeedbackMessage message = feedbackMessageBuilder.buildProgress(context, 42.5d);

    JsonObject body = toJsonObject(message).getAsJsonObject("body");

    assertEquals(TASK_IDENTIFIER, body.get("identifier").getAsString());
    assertEquals(NODE_IDENTIFIER, body.get("node").getAsString());
    assertEquals("TaskStateEnum_ACTIVE", body.get("state").getAsString());
    assertEquals(42.5d, body.get("percent_complete").getAsDouble(), 0.0001d);

    assertFalse(body.has("time_remaining"));
    assertFalse(body.has("waypoints_remaining"));
  }

  @Test
  void shouldRejectProgressLessThanZero() {
    assertThrows(
        IllegalArgumentException.class,
        () -> feedbackMessageBuilder.buildProgress(context, -0.1d));
  }

  @Test
  void shouldRejectProgressGreaterThanOneHundred() {
    assertThrows(
        IllegalArgumentException.class,
        () -> feedbackMessageBuilder.buildProgress(context, 100.1d));
  }

  @Test
  void shouldNotAllowTerminalStateInFeedbackMessage() {
    assertThrows(
        IllegalArgumentException.class,
        () -> feedbackMessageBuilder.build(context, TaskState.SUCCEEDED));
  }

  @Test
  void shouldBuildSucceededResultMessage() {
    TaskResultMessage message = resultMessageBuilder.buildSucceeded(context);

    JsonObject json = toJsonObject(message);
    JsonObject header = json.getAsJsonObject("header");
    JsonObject body = json.getAsJsonObject("body");

    assertEquals("MessageTypeEnum_TASK_RESULT", header.get("message_type").getAsString());
    assertEquals(NODE_IDENTIFIER, header.get("source").getAsString());
    assertEquals("2026-06-05T06:16:05.021Z", header.get("time_sent").getAsString());
    assertEquals("0.3.0", header.get("version").getAsString());

    assertEquals(TASK_IDENTIFIER, body.get("identifier").getAsString());
    assertEquals(NODE_IDENTIFIER, body.get("node").getAsString());
    assertEquals("TaskStateEnum_SUCCEEDED", body.get("state").getAsString());

    assertFalse(body.has("result_reason"));
  }

  @Test
  void shouldBuildRejectedResultMessageWithReason() {
    TaskResultMessage message = resultMessageBuilder.buildRejected(context, ResultReason.SAFETY);

    JsonObject body = toJsonObject(message).getAsJsonObject("body");
    JsonObject resultReason = body.getAsJsonObject("result_reason");

    assertEquals(TASK_IDENTIFIER, body.get("identifier").getAsString());
    assertEquals(NODE_IDENTIFIER, body.get("node").getAsString());
    assertEquals("TaskStateEnum_REJECTED", body.get("state").getAsString());
    assertEquals("ResultReasonEnum_SAFETY", resultReason.get("name").getAsString());
  }

  @Test
  void shouldBuildAbortedResultMessageWithReason() {
    TaskResultMessage message = resultMessageBuilder.buildAborted(context, ResultReason.HARDWARE);

    JsonObject body = toJsonObject(message).getAsJsonObject("body");
    JsonObject resultReason = body.getAsJsonObject("result_reason");

    assertEquals("TaskStateEnum_ABORTED", body.get("state").getAsString());
    assertEquals("ResultReasonEnum_HARDWARE", resultReason.get("name").getAsString());
  }

  @Test
  void shouldBuildLostResultMessageWithReason() {
    TaskResultMessage message = resultMessageBuilder.buildLost(context, ResultReason.COMMUNICATION);

    JsonObject body = toJsonObject(message).getAsJsonObject("body");
    JsonObject resultReason = body.getAsJsonObject("result_reason");

    assertEquals("TaskStateEnum_LOST", body.get("state").getAsString());
    assertEquals("ResultReasonEnum_COMMUNICATION", resultReason.get("name").getAsString());
  }

  @Test
  void shouldNotAllowNonTerminalStateInResultMessage() {
    assertThrows(
        IllegalArgumentException.class,
        () -> resultMessageBuilder.build(context, TaskState.ACTIVE, null));
  }

  private JsonObject toJsonObject(Object value) {
    return gson.toJsonTree(value).getAsJsonObject();
  }
}