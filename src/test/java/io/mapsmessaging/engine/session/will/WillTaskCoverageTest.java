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

package io.mapsmessaging.engine.session.will;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WillTaskCoverageTest {

  private final List<String> registeredIds = new ArrayList<>();

  @AfterEach
  void cleanup() {
    WillTaskManager manager = WillTaskManager.getInstance();
    registeredIds.forEach(manager::remove);
    registeredIds.clear();
  }

  @ParameterizedTest
  @MethodSource("qosPayloadCases")
  void updateQosPreservesPayloadAndRetain(
      QualityOfService initialQos,
      QualityOfService updatedQos,
      boolean retain,
      byte[] payload) {
    Message original =
        new MessageBuilder()
            .setQoS(initialQos)
            .setRetain(retain)
            .setOpaqueData(payload)
            .build();
    WillDetails details = details(original, null);
    WillTaskImpl task = new WillTaskImpl(details);

    task.updateQoS(updatedQos);

    assertEquals(updatedQos, details.getMsg().getQualityOfService());
    assertEquals(retain, details.getMsg().isRetain());
    assertArrayEquals(payload, details.getMsg().getOpaqueData());
  }

  @ParameterizedTest
  @MethodSource("retainCases")
  void updateRetainPreservesQosAndPayload(
      QualityOfService qos, boolean initialRetain, boolean updatedRetain, byte[] payload) {
    Message original =
        new MessageBuilder()
            .setQoS(qos)
            .setRetain(initialRetain)
            .setOpaqueData(payload)
            .build();
    WillDetails details = details(original, null);
    WillTaskImpl task = new WillTaskImpl(details);

    task.updateRetain(updatedRetain);

    assertEquals(qos, details.getMsg().getQualityOfService());
    assertEquals(updatedRetain, details.getMsg().isRetain());
    assertArrayEquals(payload, details.getMsg().getOpaqueData());
  }

  @ParameterizedTest
  @MethodSource("payloadReplacementCases")
  void updateMessageReplacesOnlyPayload(
      QualityOfService qos, boolean retain, byte[] originalPayload, byte[] replacementPayload) {
    Message original =
        new MessageBuilder()
            .setQoS(qos)
            .setRetain(retain)
            .setOpaqueData(originalPayload)
            .setContentType("application/octet-stream")
            .setResponseTopic("reply/topic")
            .build();
    WillDetails details = details(original, null);
    WillTaskImpl task = new WillTaskImpl(details);

    task.updateMessage(replacementPayload);

    Message updated = details.getMsg();
    assertEquals(qos, updated.getQualityOfService());
    assertEquals(retain, updated.isRetain());
    assertEquals("application/octet-stream", updated.getContentType());
    assertEquals("reply/topic", updated.getResponseTopic());
    assertArrayEquals(replacementPayload, updated.getOpaqueData());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "a",
      "sensor/temp",
      "/root/topic",
      "factory/line/1/state",
      "$SYS/server/status",
      "$schema/device",
      "topic with spaces",
      "123456789012345678901234567890",
      "a/b/c/d/e/f",
      "UPPER/lower/Mixed"
  })
  void updateTopicChangesWillDestinationWithoutManagerRegistration(String topic) {
    WillDetails details = details(message("payload"), null);
    WillTaskImpl task = new WillTaskImpl(details);

    task.updateTopic(topic);

    assertEquals(topic, details.getDestination());
    assertEquals("payload", new String(details.getMsg().getOpaqueData(), StandardCharsets.UTF_8));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "will-1",
      "will-2",
      "will-3",
      "will-4",
      "will-5",
      "will-6",
      "will-7",
      "will-8",
      "will-9",
      "will-10",
      "will-11",
      "will-12"
  })
  void managerPutGetAndRemoveMaintainsTaskIdentity(String id) {
    WillTaskManager manager = WillTaskManager.getInstance();
    WillDetails details = details(message(id), id);
    registeredIds.add(id);

    WillTaskImpl created = manager.put(id, details);

    assertSame(created, manager.get(id));
    assertSame(created, manager.remove(id));
    assertNull(manager.get(id));
    registeredIds.remove(id);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "replace-1",
      "replace-2",
      "replace-3",
      "replace-4",
      "replace-5",
      "replace-6",
      "replace-7",
      "replace-8"
  })
  void managerReplaceReturnsNewActiveTask(String id) {
    WillTaskManager manager = WillTaskManager.getInstance();
    registeredIds.add(id);
    WillDetails firstDetails = details(message("first"), id);
    WillDetails secondDetails = details(message("second"), id);

    WillTaskImpl first = manager.put(id, firstDetails);
    WillTaskImpl replacement = manager.replace(id, secondDetails);

    assertNotSame(first, replacement);
    assertSame(replacement, manager.get(id));
    assertArrayEquals(
        "second".getBytes(StandardCharsets.UTF_8),
        secondDetails.getMsg().getOpaqueData());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "cancel-1",
      "cancel-2",
      "cancel-3",
      "cancel-4",
      "cancel-5",
      "cancel-6"
  })
  void cancelBeforeSchedulingRemovesRegisteredTask(String id) {
    WillTaskManager manager = WillTaskManager.getInstance();
    registeredIds.add(id);
    WillTaskImpl task = manager.put(id, details(message("cancel"), id));

    task.cancel();

    assertNull(manager.get(id));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "managed/topic/1",
      "managed/topic/2",
      "managed/topic/3",
      "managed/topic/4",
      "managed/topic/5",
      "managed/topic/6"
  })
  void updateTopicRefreshesRegisteredManagerEntry(String topic) {
    String id = "refresh-" + topic.substring(topic.length() - 1);
    WillTaskManager manager = WillTaskManager.getInstance();
    registeredIds.add(id);
    WillDetails details = details(message("refresh"), id);
    WillTaskImpl original = manager.put(id, details);

    original.updateTopic(topic);

    assertEquals(topic, details.getDestination());
    assertNotNull(manager.get(id));
    assertNotSame(original, manager.get(id));
  }

  @Test
  void updateQosBuildsMessageWhenPreviousWillMessageIsNull() {
    WillDetails details = details(null, null);
    WillTaskImpl task = new WillTaskImpl(details);

    task.updateQoS(QualityOfService.EXACTLY_ONCE);

    assertNotNull(details.getMsg());
    assertEquals(QualityOfService.EXACTLY_ONCE, details.getMsg().getQualityOfService());
  }

  @Test
  void updateRetainBuildsMessageWhenPreviousWillMessageIsNull() {
    WillDetails details = details(null, null);
    WillTaskImpl task = new WillTaskImpl(details);

    task.updateRetain(true);

    assertNotNull(details.getMsg());
    assertTrue(details.getMsg().isRetain());
  }

  private static WillDetails details(Message message, String sessionId) {
    return new WillDetails(
        message,
        "initial/topic",
        0,
        sessionId,
        "mqtt",
        "5.0");
  }

  private static Message message(String payload) {
    return new MessageBuilder()
        .setOpaqueData(payload.getBytes(StandardCharsets.UTF_8))
        .build();
  }

  private static Stream<Arguments> qosPayloadCases() {
    List<Arguments> cases = new ArrayList<>();
    byte[][] payloads = {
        new byte[0],
        new byte[]{0},
        new byte[]{1, 2, 3},
        "hello".getBytes(StandardCharsets.UTF_8),
        new byte[32]
    };
    for (QualityOfService initial : QualityOfService.values()) {
      for (QualityOfService updated : QualityOfService.values()) {
        if (initial != updated) {
          for (byte[] payload : payloads) {
            cases.add(Arguments.of(initial, updated, true, payload));
          }
        }
      }
    }
    return cases.stream();
  }

  private static Stream<Arguments> retainCases() {
    List<Arguments> cases = new ArrayList<>();
    byte[][] payloads = {
        new byte[0],
        new byte[]{1},
        "retain".getBytes(StandardCharsets.UTF_8)
    };
    for (QualityOfService qos : QualityOfService.values()) {
      for (boolean initial : new boolean[]{false, true}) {
        for (byte[] payload : payloads) {
          cases.add(Arguments.of(qos, initial, !initial, payload));
        }
      }
    }
    return cases.stream();
  }

  private static Stream<Arguments> payloadReplacementCases() {
    return Stream.of(
        Arguments.of(QualityOfService.AT_MOST_ONCE, false, new byte[0], new byte[]{1}),
        Arguments.of(QualityOfService.AT_MOST_ONCE, true, new byte[]{1}, new byte[0]),
        Arguments.of(QualityOfService.AT_LEAST_ONCE, false, new byte[]{1, 2}, new byte[]{3, 4, 5}),
        Arguments.of(QualityOfService.AT_LEAST_ONCE, true, new byte[16], new byte[32]),
        Arguments.of(QualityOfService.EXACTLY_ONCE, false, "old".getBytes(StandardCharsets.UTF_8), "new".getBytes(StandardCharsets.UTF_8)),
        Arguments.of(QualityOfService.EXACTLY_ONCE, true, new byte[64], new byte[]{9}),
        Arguments.of(QualityOfService.MQTT_SN_REGISTERED, false, new byte[]{7, 8}, new byte[]{9, 10}),
        Arguments.of(QualityOfService.MQTT_SN_REGISTERED, true, new byte[128], new byte[3])
    );
  }
}
