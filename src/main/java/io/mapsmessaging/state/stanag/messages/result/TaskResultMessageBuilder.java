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

package io.mapsmessaging.state.stanag.messages.result;

import io.mapsmessaging.state.stanag.messages.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TaskResultMessageBuilder {

  private final MessageHeaderBuilder headerBuilder;
  private final ResultReasonBuilder resultReasonBuilder;

  public TaskResultMessage buildSucceeded(TaskStatusContext context) {
    return build(context, TaskState.SUCCEEDED, null);
  }

  public TaskResultMessage buildAccepted(TaskStatusContext context) {
    return build(context, TaskState.ACTIVE, null);
  }

  public TaskResultMessage buildRejected(TaskStatusContext context, ResultReason resultReason) {
    return build(context, TaskState.REJECTED, resultReason);
  }

  public TaskResultMessage buildAborted(TaskStatusContext context, ResultReason resultReason) {
    return build(context, TaskState.ABORTED, resultReason);
  }

  public TaskResultMessage buildLost(TaskStatusContext context, ResultReason resultReason) {
    return build(context, TaskState.LOST, resultReason);
  }

  public TaskResultMessage buildRecalled(TaskStatusContext context, ResultReason resultReason) {
    return build(context, TaskState.RECALLED, resultReason);
  }

  public TaskResultMessage buildPreempted(TaskStatusContext context, ResultReason resultReason) {
    return build(context, TaskState.PREEMPTED, resultReason);
  }

  public TaskResultMessage build(TaskStatusContext context, TaskState taskState, ResultReason resultReason) {
    TaskResultBody body = new TaskResultBody(context.taskIdentifier(), context.nodeIdentifier(), taskState, context.authority(), resultReasonBuilder.build(resultReason));
    MessageHeader header = headerBuilder.build(MessageType.TASK_RESULT, context.nodeIdentifier().toString());
    return new TaskResultMessage(header, body);
  }
}