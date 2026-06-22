package io.mapsmessaging.state.stanag.messages.task.admin;

import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TaskAdminMessage {

  private final MessageHeader header;

  private final TaskAdminBody body;
}