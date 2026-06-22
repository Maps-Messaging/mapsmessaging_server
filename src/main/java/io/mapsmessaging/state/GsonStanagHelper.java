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

package io.mapsmessaging.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.rest.translation.GsonDateTimeDeserialiser;
import io.mapsmessaging.rest.translation.GsonDateTimeSerialiser;
import io.mapsmessaging.rest.translation.InstantTypeAdapter;
import io.mapsmessaging.state.config.capability.*;
import io.mapsmessaging.state.stanag.messages.core.MessageType;
import io.mapsmessaging.state.stanag.messages.task.admin.TaskAdminActionEnum;
import io.mapsmessaging.state.stanag.messages.task.result.ResultReason;
import io.mapsmessaging.state.stanag.messages.TaskState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class GsonStanagHelper {

  public static Gson createGson() {
    return new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeSerialiser())
        .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeDeserialiser())
        .registerTypeAdapter(LocalDate.class, new GsonDateTimeSerialiser())
        .registerTypeAdapter(LocalDate.class, new GsonDateTimeDeserialiser())
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(
            PlanTaskType.class,
            new PrefixedEnumTypeAdapter<>(PlanTaskType.class, "PlanTaskTypeEnum_")
        )
        .registerTypeAdapter(
            TaskSpecialization.class,
            new PrefixedEnumTypeAdapter<>(TaskSpecialization.class, "TaskSpecializationEnum_")
        )
        .registerTypeAdapter(
            TaskConditionMode.class,
            new PrefixedEnumTypeAdapter<>(TaskConditionMode.class, "TaskConditionModeEnum_")
        )
        .registerTypeAdapter(
            TaskTemplateMode.class,
            new PrefixedEnumTypeAdapter<>(TaskTemplateMode.class, "TaskTemplateModeEnum_")
        )
        .registerTypeAdapter(
            MessageType.class,
            new PrefixedEnumTypeAdapter<>(MessageType.class, "MessageTypeEnum_")
        )
        .registerTypeAdapter(
            TaskState.class,
            new PrefixedEnumTypeAdapter<>(TaskState.class, "TaskStateEnum_")
        )
        .registerTypeAdapter(
            ResultReason.class,
            new PrefixedEnumTypeAdapter<>(ResultReason.class, "ResultReasonEnum_")
        )
        .registerTypeAdapter(
            TaskAdminActionEnum.class,
            new PrefixedEnumTypeAdapter<>(TaskAdminActionEnum.class, "TaskAdminActionEnum_")
        )
        .create();
  }

  private GsonStanagHelper() {
    // helper only
  }
}
