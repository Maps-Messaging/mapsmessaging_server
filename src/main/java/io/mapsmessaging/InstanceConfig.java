/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging;


import io.mapsmessaging.auth.registry.PasswordGenerator;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.security.uuid.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.mapsmessaging.logging.ServerLogMessages.INSTANCE_STATE_ERROR;

/**
 * This is the InstanceConfig class.
 * It represents the configuration for an instance of a server.
 * The class has properties for serverName, uuid, and creationDate.
 * It also has methods for loading and saving the state of the instance configuration.
 */
public class InstanceConfig {


  private static final String INSTANCE_CONFIG_YAML = "InstanceConfig.yaml";

  private final Logger logger = LoggerFactory.getLogger(InstanceConfig.class);

  @Getter
  @Setter
  private String serverName;

  @Getter
  @Setter
  private UUID uuid;

  @Getter
  @Setter
  private String creationDate = LocalDateTime.now().toString();

  @Getter
  @Setter
  private String secureTokenSecret;


  private final String path;

  public  InstanceConfig(){
    path ="";
  }

  /**
   * Constructor for the InstanceConfig class.
   * Initializes the path and sets the serverName to null.
   *
   * @param path The path for the instance configuration.
   */
  public InstanceConfig(String path){
    this.path= path;
    serverName = null;
  }

  /**
   * Loads the state of the instance configuration.
   *
   * This method reads the instance configuration file and populates the serverName, creationDate, and uuid properties
   * of the InstanceConfig object. If the uuid property is null, a new UUID is generated using the UuidGenerator class
   * and the state is saved using the saveState method.
   *
   */
  public void loadState() {
    LoaderOptions options = new LoaderOptions();
    options.setTagInspector(tag -> true);
    Yaml yaml = new Yaml(new Constructor(options));
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(path + INSTANCE_CONFIG_YAML);
      InstanceConfig obj;
      obj = yaml.loadAs(fileInputStream, InstanceConfig.class);
      fileInputStream.close();
      serverName = obj.serverName;
      creationDate = obj.creationDate;
      boolean saveState = false;
      if (obj.uuid == null) {
        uuid = UuidGenerator.getInstance().generate();
        saveState = true;
      } else {
        uuid = obj.getUuid();
      }
      if(obj.secureTokenSecret == null){
        secureTokenSecret = PasswordGenerator.generateRandomPassword(64);
        saveState = true;
      }
      else{
        secureTokenSecret = obj.secureTokenSecret;
      }
      if(saveState){
        saveState();
      }
    } catch (IOException e) {
      // if the first run, then lets allocate a new UUID
      uuid = UuidGenerator.getInstance().generate();
      secureTokenSecret = PasswordGenerator.generateRandomPassword(64);
    }
  }

  /**
   * Saves the current state of the InstanceConfig object to a YAML file.
   *
   * This method uses the SnakeYAML library to serialize the object to YAML format and write it to a file.
   * The file path is determined by the 'path' field of the InstanceConfig object, appended with the constant INSTANCE_CONFIG_YAML.
   *
   */
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
