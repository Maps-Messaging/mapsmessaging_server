package io.mapsmessaging;


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

  private static final String fileName = "InstanceConfig.yaml";

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

  public boolean loadState(){
    Yaml yaml = new Yaml();
    Object obj = null;
    try {
      FileInputStream fileInputStream = new FileInputStream(path+fileName);
      obj = yaml.load(fileInputStream);
    } catch (FileNotFoundException e) {
      //
    }
    if(obj instanceof InstanceConfig){
      serverName = ((InstanceConfig)obj).serverName;
      creationDate = ((InstanceConfig)obj).creationDate;
      return true;
    }
    return false;
  }

  public void saveState(){
    Yaml yaml = new Yaml();
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(path+fileName);
      OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
      yaml.dump(this, writer);
      fileOutputStream.close();
    } catch (IOException e) {

    }
  }
}
