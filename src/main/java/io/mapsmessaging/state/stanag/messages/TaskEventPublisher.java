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

package io.mapsmessaging.state.stanag.messages;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.state.GsonStanagHelper;
import io.mapsmessaging.state.stanag.StanagSession;
import io.mapsmessaging.state.stanag.messages.admin.TaskAdminMessage;
import io.mapsmessaging.state.stanag.messages.feedback.TaskFeedbackMessage;
import io.mapsmessaging.state.stanag.messages.result.TaskResultMessage;
import io.mapsmessaging.state.stanag.tasks.monitor.TaskMonitor;

import java.nio.charset.StandardCharsets;

public class TaskEventPublisher {

  private static final String TASK_ADMIN_SCHEMA_NAME = "catl.hibw.messages.task.TaskAdmin";
  private static final String TASK_FEEDBACK_SCHEMA_NAME = "catl.hibw.messages.task.TaskFeedback";
  private static final String TASK_RESULT_SCHEMA_NAME = "catl.hibw.messages.task.TaskResult";

  private final StanagSession protocol;
  private final Gson gson;
  private final TaskSchemaValidator schemaValidator;

  public TaskEventPublisher(StanagSession protocol, TaskSchemaValidator schemaValidator) {
    this.protocol = protocol;
    this.gson = GsonStanagHelper.createGson();
    this.schemaValidator = schemaValidator;
  }

  public void publishFeedback(TaskMonitor taskMonitor, TaskFeedbackMessage taskFeedbackMessage) {
    String topicPath = taskMonitor.getTopicPath() + "/MessageTypeEnum_TASK_FEEDBACK";
    JsonObject jsonObject = gson.toJsonTree(taskFeedbackMessage).getAsJsonObject();

    schemaValidator.validate(TASK_FEEDBACK_SCHEMA_NAME, jsonObject);
    publishTaskEvent(topicPath, jsonObject);
  }

  public void publishResult(TaskMonitor taskMonitor, TaskResultMessage taskResultMessage) {
    String topicPath = taskMonitor.getTopicPath() + "/MessageTypeEnum_TASK_RESULT";
    JsonObject jsonObject = gson.toJsonTree(taskResultMessage).getAsJsonObject();

    schemaValidator.validate(TASK_RESULT_SCHEMA_NAME, jsonObject);
    publishTaskEvent(topicPath, jsonObject);
  }

  public void publishAdmin(TaskMonitor taskMonitor, TaskAdminMessage taskAdminMessage) {
    String topicPath = taskMonitor.getTopicPath() + "/MessageTypeEnum_TASK_ADMIN";
    JsonObject jsonObject = gson.toJsonTree(taskAdminMessage).getAsJsonObject();

    schemaValidator.validate(TASK_ADMIN_SCHEMA_NAME, jsonObject);
    publishTaskEvent(topicPath, jsonObject);
  }

  private void publishTaskEvent(String topicPath, JsonObject jsonObject) {
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setOpaqueData(jsonObject.toString().getBytes(StandardCharsets.UTF_8));

    protocol.sendTaskMessage(topicPath, messageBuilder.build());
  }
}