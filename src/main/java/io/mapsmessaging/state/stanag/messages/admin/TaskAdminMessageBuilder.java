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

package io.mapsmessaging.state.stanag.messages.admin;


import io.mapsmessaging.state.config.capability.Authorities;
import io.mapsmessaging.state.stanag.messages.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.MessageType;
import io.mapsmessaging.state.stanag.messages.TaskStatusContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TaskAdminMessageBuilder {

  private final MessageHeaderBuilder headerBuilder;


  public TaskAdminMessage buildAssign(TaskStatusContext context) {
    return build(context, TaskAdminActionEnum.ASSIGN, context.authority());
  }

  public TaskAdminMessage build(TaskStatusContext context, TaskAdminActionEnum taskState, Authorities authority) {
    TaskAdminBody body = new TaskAdminBody(context.taskIdentifier().toString(), context.nodeIdentifier().toString(), taskState, authority);
    return new TaskAdminMessage(headerBuilder.build(MessageType.TASK_ADMIN, context.nodeIdentifier().toString()), body);
  }
}
