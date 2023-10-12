/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging;


import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TrustedTagInspector;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.time.LocalDateTime;

import static io.mapsmessaging.logging.ServerLogMessages.INSTANCE_STATE_ERROR;

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

  public void loadState() {
    LoaderOptions options = new LoaderOptions();
    options.setTagInspector(new TrustedTagInspector());
    Yaml yaml = new Yaml(new Constructor(options), new Representer(new DumperOptions()));
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(path + INSTANCE_CONFIG_YAML);
      InstanceConfig obj;
      obj = yaml.loadAs(fileInputStream, InstanceConfig.class);
      serverName = obj.serverName;
      creationDate = obj.creationDate;
    } catch (FileNotFoundException e) {
//      throw new RuntimeException(e);
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
