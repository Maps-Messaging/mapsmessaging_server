package io.mapsmessaging.state.stanag.messages.task.result;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.state.stanag.messages.TaskState;
import io.mapsmessaging.state.stanag.messages.TaskStatusContext;
import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import io.mapsmessaging.state.stanag.messages.core.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.core.MessageType;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@RequiredArgsConstructor
public class TaskResultMessageBuilder {

  private final MessageHeaderBuilder headerBuilder;

  private final ResultReasonBuilder resultReasonBuilder;

  public TaskResultMessage buildSucceeded(TaskStatusContext context) {
    return build(context, TaskState.SUCCEEDED, null, null);
  }

  public TaskResultMessage buildRejected(TaskStatusContext context, ResultReason resultReason, String reasonText) {
    return build(context, TaskState.REJECTED, resultReason, reasonText);
  }

  public TaskResultMessage buildAborted(TaskStatusContext context, ResultReason resultReason,String reasonText) {
    return build(context, TaskState.ABORTED, resultReason, reasonText);
  }

  public TaskResultMessage buildLost(TaskStatusContext context, ResultReason resultReason,String reasonText) {
    return build(context, TaskState.LOST, resultReason, reasonText);
  }

  public TaskResultMessage buildRecalled(TaskStatusContext context, ResultReason resultReason,String reasonText) {
    return build(context, TaskState.RECALLED, resultReason, reasonText);
  }

  public TaskResultMessage buildPreempted(TaskStatusContext context, ResultReason resultReason,String reasonText) {
    return build(context, TaskState.PREEMPTED, resultReason, reasonText);
  }

  public TaskResultMessage build(TaskStatusContext context, TaskState taskState, ResultReason resultReason, String reasonText) {
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(taskState, "taskState cannot be null");

    validateResultState(taskState);

    MessageHeader header = headerBuilder.build(MessageType.TASK_RESULT, context.nodeIdentifier(), Instant.now());
    TaskResultBody body = TaskResultBody.builder()
        .identifier(context.taskIdentifier())
        .node(context.nodeIdentifier())
        .state(taskState)
        .timestamp(Instant.now())
        .resultReason(resultReasonBuilder.build(resultReason, reasonText))
        .build();

    return new TaskResultMessage(header, body);
  }

  private void validateResultState(TaskState taskState) {
    if (!taskState.isTerminal()) {
      throw new IllegalArgumentException("Non-terminal task states must use TASK_FEEDBACK: " + taskState);
    }
  }
}