package io.mapsmessaging.state.stanag.messages.task.feedback;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.state.stanag.messages.TaskState;
import io.mapsmessaging.state.stanag.messages.TaskStatusContext;
import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import io.mapsmessaging.state.stanag.messages.core.MessageHeaderBuilder;
import io.mapsmessaging.state.stanag.messages.core.MessageType;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class TaskFeedbackMessageBuilder {

  private final MessageHeaderBuilder headerBuilder;

  public TaskFeedbackMessage buildActive(TaskStatusContext context) {
    return build(context, TaskState.ACTIVE);
  }

  public TaskFeedbackMessage buildProgress(TaskStatusContext context, double percentComplete) {
    return build(context, TaskState.ACTIVE, percentComplete, null, null);
  }

  public TaskFeedbackMessage buildWaypointsRemaining(
      TaskStatusContext context,
      List<String> waypointsRemaining) {

    return build(context, TaskState.ACTIVE, null, null, waypointsRemaining);
  }

  public TaskFeedbackMessage build(TaskStatusContext context, TaskState taskState) {
    return build(context, taskState, null, null, null);
  }

  public TaskFeedbackMessage build(
      TaskStatusContext context,
      TaskState taskState,
      Double percentComplete,
      String timeRemaining,
      List<String> waypointsRemaining) {

    Objects.requireNonNull(context, "context cannot be null");
    Objects.requireNonNull(taskState, "taskState cannot be null");

    validateFeedbackState(taskState);
    validatePercentComplete(percentComplete);

    MessageHeader header = headerBuilder.build(MessageType.TASK_FEEDBACK, context.nodeIdentifier(), Instant.now());

    TaskFeedbackBody.TaskFeedbackBodyBuilder bodyBuilder = TaskFeedbackBody.builder()
        .identifier(context.taskIdentifier())
        .node(context.nodeIdentifier())
        .state(taskState);

    if (percentComplete != null) {
      bodyBuilder.percentComplete(percentComplete);
    }

    if (timeRemaining != null) {
      bodyBuilder.timeRemaining(timeRemaining);
    }

    if (waypointsRemaining != null && !waypointsRemaining.isEmpty()) {
      bodyBuilder.waypointsRemaining(waypointsRemaining);
    }

    return new TaskFeedbackMessage(header, bodyBuilder.build());
  }

  private void validatePercentComplete(Double percentComplete) {
    if (percentComplete == null) {
      return;
    }

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