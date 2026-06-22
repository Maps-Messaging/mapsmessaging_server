package io.mapsmessaging.state.stanag.messages.task.result;

import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskResultMessage {

  private final MessageHeader header;

  private final TaskResultBody body;
}