package io.mapsmessaging.engine.tasks;

import io.mapsmessaging.api.MessageEvent;

public class MessageResponse implements Response {

  private final MessageEvent response;

  public MessageResponse(MessageEvent value) {
    response = value;
  }

  public MessageEvent getResponse() {
    return response;
  }

}
