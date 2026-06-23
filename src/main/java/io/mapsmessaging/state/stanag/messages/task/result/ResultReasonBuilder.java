package io.mapsmessaging.state.stanag.messages.task.result;

import io.mapsmessaging.state.stanag.messages.FlexibleEnumeration;

public class ResultReasonBuilder {

  public FlexibleEnumeration build(ResultReason resultReason, String text) {
    if (resultReason == null) {
      return null;
    }

    if (text != null) {
      return new FlexibleEnumeration(resultReason.name()+" "+text);
    }
    return new FlexibleEnumeration(resultReason.name());
  }
}