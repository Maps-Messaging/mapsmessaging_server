package io.mapsmessaging.state.stanag.messages.task.result;

import io.mapsmessaging.state.stanag.messages.FlexibleEnumeration;

public class ResultReasonBuilder {

  public FlexibleEnumeration build(ResultReason resultReason) {
    if (resultReason == null) {
      return null;
    }

    return new FlexibleEnumeration(resultReason.name());
  }
}