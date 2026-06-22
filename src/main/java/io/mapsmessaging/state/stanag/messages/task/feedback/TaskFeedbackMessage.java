package io.mapsmessaging.state.stanag.messages.task.feedback;

import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskFeedbackMessage {

  private final MessageHeader header;

  private final TaskFeedbackBody body;
}