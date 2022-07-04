package io.mapsmessaging.engine.system.impl;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopic;
import java.io.IOException;
import java.util.UUID;

public class NotImplemented extends SystemTopic {

  public NotImplemented() throws IOException {
    super("$SYS/notImplemented");
  }

  @Override
  public UUID getSchemaUUID() {
    return SchemaManager.DEFAULT_STRING_SCHEMA;
  }

  @Override
  protected Message generateMessage() {
    return getMessage("0".getBytes());
  }
}