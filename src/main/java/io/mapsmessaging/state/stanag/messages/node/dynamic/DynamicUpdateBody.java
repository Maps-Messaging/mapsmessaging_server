package io.mapsmessaging.state.stanag.messages.node.dynamic;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DynamicUpdateBody {

  private final DynamicUpdateOperation operation;

}