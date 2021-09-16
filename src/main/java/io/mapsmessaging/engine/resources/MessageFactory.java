package io.mapsmessaging.engine.resources;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.storage.Factory;

public class MessageFactory implements Factory<Message> {

  @Override
  public Message create() {
    return new Message();
  }
}
