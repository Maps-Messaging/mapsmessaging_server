package io.mapsmessaging.engine.schema;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.formatters.impl.RawFormatter;
import java.io.IOException;
import java.util.UUID;
import lombok.Getter;

public class Schema {

  @Getter
  private final UUID uniqueId;

  @Getter
  private Format format;

  public Schema(SchemaConfig config){
    uniqueId = config.getUniqueId();
    format = buildFormat(config);
  }

  public boolean update(Schema rhs){
    format = rhs.getFormat();
    return true;
  }

  private Format buildFormat(SchemaConfig config){
    try {
      MessageFormatter messageFormatter = MessageFormatterFactory.getInstance().getFormatter(config);
      return new Format(messageFormatter);
    } catch (IOException e) {
      return new Format(new RawFormatter());
    }
  }

}
