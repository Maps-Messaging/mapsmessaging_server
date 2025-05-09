package io.mapsmessaging.engine.tasks;

import io.mapsmessaging.api.message.Message;

public class MessageResponse implements Response {

  private final Message response;

  public MessageResponse(Message value) {
    response = value;
  }

  public Message getResponse() {
    return response;
  }

}
