package io.mapsmessaging.state.stanag.messages.node.dynamic;

import io.mapsmessaging.state.stanag.messages.core.MessageHeader;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DynamicUpdate {

  private final MessageHeader header;

  private final DynamicUpdateBody body;
}