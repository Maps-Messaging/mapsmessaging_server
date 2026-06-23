package io.mapsmessaging.state.stanag.messages.task.admin;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.state.config.capability.Authorities;
import io.mapsmessaging.state.stanag.messages.TaskStatusContext;
import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import io.mapsmessaging.state.stanag.messages.core.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.core.MessageType;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@RequiredArgsConstructor
public class TaskAdminMessageBuilder {

  private final MessageHeaderBuilder headerBuilder;

  public TaskAdminMessage build(TaskStatusContext context, TaskAdminActionEnum action, Authorities authority) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(action, "action cannot be null");

    MessageHeader header = headerBuilder.build(MessageType.TASK_ADMIN, context.nodeIdentifier() , Instant.now());

    TaskAdminBody body = TaskAdminBody.builder()
        .identifier(context.taskIdentifier())
        .node(context.nodeIdentifier())
        .action(action)
        .authority(authority)
        .build();

    return new TaskAdminMessage(header, body);
  }
}