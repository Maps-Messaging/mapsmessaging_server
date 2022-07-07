package io.mapsmessaging.api.message;

import io.mapsmessaging.storage.StorableFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class MessageFactory implements StorableFactory<Message> {

  private static MessageFactory instance;

  static {
    instance = new MessageFactory();
  }

  public static MessageFactory getInstance() {
    return instance;
  }

  @Override
  public @NotNull Message unpack(@NotNull ByteBuffer[] byteBuffers) throws IOException {
    return new Message(byteBuffers);
  }

  @Override
  public @NotNull ByteBuffer[] pack(@NotNull Message message) throws IOException {
    return message.pack();
  }
}
