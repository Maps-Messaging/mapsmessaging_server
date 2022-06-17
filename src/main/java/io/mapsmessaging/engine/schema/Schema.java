package io.mapsmessaging.engine.schema;

import io.mapsmessaging.api.message.format.Format;
import io.mapsmessaging.api.message.format.FormatManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import lombok.Getter;
import org.json.JSONObject;

public class Schema {

  @Getter
  private Format format;

  public Schema(String format){
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("format", format);
    this.format = buildFormat(props);
  }

  public Schema(ConfigurationProperties props){
    format = buildFormat(props);
  }

  public boolean update(Schema rhs){
    format = rhs.getFormat();
    return true;
  }

  private Format buildFormat(ConfigurationProperties props){
    Format tmp = FormatManager.getInstance().getFormat(props.getProperty("format", "RAW"));
    return tmp.getInstance(props);
  }

}
