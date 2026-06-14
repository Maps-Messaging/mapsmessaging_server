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

package io.mapsmessaging.state.config.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

class PrefixedEnumTypeAdapterTest {

  private final PrefixedEnumTypeAdapter<TaskConditionMode> adapter =
      new PrefixedEnumTypeAdapter<>(TaskConditionMode.class, "CONDITION_MODE_");

  @Test
  void serialize_enumValue_addsPrefix() {
    assertEquals(
        "CONDITION_MODE_COMPLEX",
        adapter.serialize(TaskConditionMode.COMPLEX, TaskConditionMode.class, null).getAsString()
    );
  }

  @Test
  void serialize_null_returnsJsonNull() {
    assertTrue(adapter.serialize(null, TaskConditionMode.class, null).isJsonNull());
  }

  @Test
  void deserialize_prefixedAndUnprefixedValues_returnsEnum() {
    assertEquals(
        TaskConditionMode.FOLLOW_TASK_STATE,
        adapter.deserialize(new JsonPrimitive("CONDITION_MODE_FOLLOW_TASK_STATE"), TaskConditionMode.class, null)
    );
    assertEquals(
        TaskConditionMode.FOLLOW_TASK_STATE,
        adapter.deserialize(new JsonPrimitive("FOLLOW_TASK_STATE"), TaskConditionMode.class, null)
    );
  }

  @Test
  void deserialize_nullValues_returnsNull() {
    assertNull(adapter.deserialize(null, TaskConditionMode.class, null));
    assertNull(adapter.deserialize(JsonNull.INSTANCE, TaskConditionMode.class, null));
  }

  @Test
  void deserialize_unknownOrWrongCaseValue_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adapter.deserialize(new JsonPrimitive("CONDITION_MODE_UNKNOWN"), TaskConditionMode.class, null)
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> adapter.deserialize(new JsonPrimitive("condition_mode_complex"), TaskConditionMode.class, null)
    );
  }
}
