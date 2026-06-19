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

package io.mapsmessaging.state.stanag.messages.feedback;

import io.mapsmessaging.state.stanag.messages.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TaskFeedbackMessageBuilder {

  private final MessageHeaderBuilder headerBuilder;

  public TaskFeedbackMessage buildAccepted(TaskStatusContext context) {
    return build(context, TaskState.ACTIVE);
  }

  public TaskFeedbackMessage buildActive(TaskStatusContext context) {
    return build(context, TaskState.ACTIVE);
  }

  public TaskFeedbackMessage buildProgress(TaskStatusContext context, double percentComplete) {
    TaskFeedbackDetails details = new TaskFeedbackDetails(percentComplete, null, null);
    return build(context, TaskState.ACTIVE, details);
  }

  public TaskFeedbackMessage buildWaypointsRemaining(
      TaskStatusContext context,
      List<String> waypointsRemaining) {

    TaskFeedbackDetails details = new TaskFeedbackDetails(0.0, null, waypointsRemaining);
    return build(context, TaskState.ACTIVE, details);
  }

  public TaskFeedbackMessage build(TaskStatusContext context, TaskState taskState) {
    return build(context, taskState, new TaskFeedbackDetails());
  }

  public TaskFeedbackMessage build(TaskStatusContext context, TaskState taskState, TaskFeedbackDetails details) {

    validateFeedbackState(taskState);
    validateDetails(details);

    TaskFeedbackBody.TaskFeedbackBodyBuilder bodyBuilder =
        TaskFeedbackBody.builder()
            .identifier(context.taskIdentifier())
            .node(context.nodeIdentifier())
            .state(taskState);

    if (details != null) {
      applyDetails(bodyBuilder, details);
    }
    MessageHeader header = headerBuilder.build(MessageType.TASK_FEEDBACK, context.nodeIdentifier().toString());
    return new TaskFeedbackMessage(header, bodyBuilder.build());
  }

  private void applyDetails(
      TaskFeedbackBody.TaskFeedbackBodyBuilder bodyBuilder,
      TaskFeedbackDetails details) {

    bodyBuilder.percentComplete(details.getPercentComplete());

//    if (details.getTimeRemaining() != null) {
//      bodyBuilder.timeRemaining(details.getTimeRemaining());
//    }

    if (details.getWaypointsRemaining() != null && !details.getWaypointsRemaining().isEmpty()) {
      bodyBuilder.waypointsRemaining(details.getWaypointsRemaining());
    }
  }

  private void validateDetails(TaskFeedbackDetails details) {
    if (details == null) {
      return;
    }

    double percentComplete = details.getPercentComplete();

    if (percentComplete < 0 || percentComplete > 100) {
      throw new IllegalArgumentException("percent_complete must be between 0 and 100");
    }
  }

  private void validateFeedbackState(TaskState taskState) {
    if (taskState.isTerminal()) {
      throw new IllegalArgumentException("Terminal task states must use TASK_RESULT: " + taskState);
    }
  }
}