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
    return build(context, TaskState.SUCCEEDED, null);
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
    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(taskState, "taskState cannot be null");

    validateResultState(taskState);

    MessageHeader header = headerBuilder.build(MessageType.TASK_RESULT, MessageDaemon.getInstance().getUuid(), Instant.now());

    TaskResultBody body = TaskResultBody.builder()
        .identifier(context.taskIdentifier())
        .node(context.nodeIdentifier())
        .state(taskState)
        .authority(context.authority())
        .resultReason(resultReasonBuilder.build(resultReason))
        .build();

    return new TaskResultMessage(header, body);
  }

  private void validateResultState(TaskState taskState) {
    if (!taskState.isTerminal()) {
      throw new IllegalArgumentException("Non-terminal task states must use TASK_FEEDBACK: " + taskState);
    }
  }
}