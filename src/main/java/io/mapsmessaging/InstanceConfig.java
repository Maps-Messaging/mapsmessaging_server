package io.mapsmessaging;


import static io.mapsmessaging.logging.ServerLogMessages.INSTANCE_STATE_ERROR;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;

public class InstanceConfig {


  private static final String INSTANCE_CONFIG_YAML = "InstanceConfig.yaml";

  private final Logger logger = LoggerFactory.getLogger(InstanceConfig.class);

  @Getter
  @Setter
  private String serverName;

  @Getter
  @Setter
  private String creationDate = LocalDateTime.now().toString();

  private final String path;

  public  InstanceConfig(){
    path ="";
  }

  public InstanceConfig(String path){
    this.path= path;
    serverName = null;
  }

  public void loadState(){
    Yaml yaml = new Yaml();
    Object obj = null;
    try {
      FileInputStream fileInputStream = new FileInputStream(path+ INSTANCE_CONFIG_YAML);
      obj = yaml.load(fileInputStream);
    } catch (FileNotFoundException e) {
      logger.log(INSTANCE_STATE_ERROR, path+ INSTANCE_CONFIG_YAML, e);
    }
    if(obj instanceof InstanceConfig){
      serverName = ((InstanceConfig)obj).serverName;
      creationDate = ((InstanceConfig)obj).creationDate;
    }
  }

  public void saveState(){
    Yaml yaml = new Yaml();
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(path+ INSTANCE_CONFIG_YAML);
      OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
      yaml.dump(this, writer);
      fileOutputStream.close();
    } catch (IOException e) {
      logger.log(INSTANCE_STATE_ERROR, path+ INSTANCE_CONFIG_YAML, e);
    }
  }
}
