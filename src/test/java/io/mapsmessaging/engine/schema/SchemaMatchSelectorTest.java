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
 */

package io.mapsmessaging.engine.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaMatchSelectorTest {

  @Test
  void exactNameBeatsLooseSuffixRegardlessOfIterationOrder() {
    SchemaConfig predicted = schema("base.task_feedback.PredictTaskFeedback", "predict-id", "protobuf");
    SchemaConfig exact = schema("TaskFeedback", "task-feedback-id", "protobuf");

    assertEquals(
        exact,
        SchemaMatchSelector.select("TaskFeedback", "protobuf", null, List.of(predicted, exact)));
  }

  @Test
  void exactNameBeatsStalePreloadedAlias() {
    SchemaConfig preloaded = schema("base.task_feedback.PredictTaskFeedback", "predict-id", "protobuf");
    SchemaConfig exact = schema("TaskFeedback", "task-feedback-id", "protobuf");

    assertEquals(
        exact,
        SchemaMatchSelector.select("TaskFeedback", "protobuf", preloaded, List.of(preloaded, exact)));
  }

  @Test
  void exactUniqueIdBeatsPreloadedAlias() {
    SchemaConfig preloaded = schema("SomethingElse", "alias-target", "protobuf");
    SchemaConfig exactId = schema("OtherMessage", "TaskFeedback", "protobuf");

    assertEquals(
        exactId,
        SchemaMatchSelector.select("TaskFeedback", "protobuf", preloaded, List.of(preloaded, exactId)));
  }

  @Test
  void qualifiedSuffixBeatsLooseSuffix() {
    SchemaConfig loose = schema("PredictTaskFeedback", "loose", "protobuf");
    SchemaConfig qualified = schema("base.task_feedback.TaskFeedback", "qualified", "protobuf");

    assertEquals(
        qualified,
        SchemaMatchSelector.select("TaskFeedback", "protobuf", null, List.of(loose, qualified)));
  }

  @Test
  void preloadedAliasBeatsLooseSuffixWhenNoExactMatchExists() {
    SchemaConfig preloaded = schema("base.alias.SelectedMessage", "selected", "protobuf");
    SchemaConfig loose = schema("PredictTaskFeedback", "loose", "protobuf");

    assertEquals(
        preloaded,
        SchemaMatchSelector.select("TaskFeedback", "protobuf", preloaded, List.of(loose, preloaded)));
  }

  @Test
  void wrongFormatIsNeverReturned() {
    SchemaConfig json = schema("TaskFeedback", "json-id", "json");

    assertNull(SchemaMatchSelector.select("TaskFeedback", "protobuf", json, List.of(json)));
  }

  @Test
  void shorterQualifiedNameWinsEqualRank() {
    SchemaConfig longer = schema("very.long.base.task_feedback.TaskFeedback", "long", "protobuf");
    SchemaConfig shorter = schema("base.TaskFeedback", "short", "protobuf");

    assertEquals(
        shorter,
        SchemaMatchSelector.select("TaskFeedback", "protobuf", null, List.of(longer, shorter)));
  }

  private SchemaConfig schema(String name, String uniqueId, String format) {
    SchemaConfig schema = mock(SchemaConfig.class);
    when(schema.getName()).thenReturn(name);
    when(schema.getUniqueId()).thenReturn(uniqueId);
    when(schema.getFormat()).thenReturn(format);
    return schema;
  }
}
