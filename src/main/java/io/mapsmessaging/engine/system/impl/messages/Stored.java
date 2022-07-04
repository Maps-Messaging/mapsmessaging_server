package io.mapsmessaging.engine.system.impl.messages;

import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopicWithAverage;
import java.io.IOException;
import java.util.UUID;

public class Stored extends SystemTopicWithAverage {

  public Stored() throws IOException {
    super("$SYS/broker/messages/stored", true);
  }

  @Override
  public UUID getSchemaUUID() {
    return SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA;
  }

  @Override
  public long getData() {
    return 0;
  }

  @Override
  public String[] aliases() {
    return new String[]{
        "$SYS/messages/stored"
    };
  }
}