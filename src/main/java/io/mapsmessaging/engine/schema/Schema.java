package io.mapsmessaging.engine.schema;

import io.mapsmessaging.api.message.format.Format;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.formatters.RawFormatter;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import lombok.Getter;

public class Schema {

  @Getter
  private Format format;

  public Schema(String format){
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("format", format);
    this.format = buildFormat(props);
  }

  public Schema(ConfigurationProperties props){
    ConfigurationProperties schemaProps = new ConfigurationProperties();
    schemaProps.put("schema", props);
    format = buildFormat(schemaProps);
  }

  public boolean update(Schema rhs){
    format = rhs.getFormat();
    return true;
  }

  private Format buildFormat(ConfigurationProperties props){
    try {
      SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(props);
      MessageFormatter messageFormatter = MessageFormatterFactory.getInstance().getFormatter(config);
      return new Format(messageFormatter);
    } catch (IOException e) {
      return new Format(new RawFormatter());
    }
  }

}
