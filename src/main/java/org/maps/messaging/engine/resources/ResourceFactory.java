/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.engine.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.UUID;
import org.maps.messaging.BuildInfo;
import org.maps.messaging.api.features.DestinationType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ResourceFactory {
  public final static String RESOURCE_FILE_NAME = "resource.yaml";

  private static final ResourceFactory instance = new ResourceFactory();

  private enum RESOURCE_TYPE  {DATABASE, RANDOM_ACCESS_FILE, CHANNEL, MEMORY}

  private final RESOURCE_TYPE type;


  private ResourceFactory() {
    String config = System.getProperty("TYPE", "DATABASE").toLowerCase();
    switch (config) {
      case "random":
        type = RESOURCE_TYPE.RANDOM_ACCESS_FILE;
        break;
      case "database":
        type = RESOURCE_TYPE.DATABASE;
        break;

      case "memory":
        type = RESOURCE_TYPE.MEMORY;
        break;

      case "channel":
      default:
        type = RESOURCE_TYPE.CHANNEL;
        break;
    }
  }

  public static ResourceFactory getInstance() {
    return instance;
  }

  public Resource create(String path, String resourceName, UUID uuid, DestinationType destinationType) throws IOException {
    if (resourceName.toLowerCase().startsWith("$sys")) {
      return new MemoryResource(resourceName);
    } else {
      createMetaData(path, resourceName, uuid, destinationType);
      return createPersistentResource(path, resourceName, uuid);
    }
  }

  public Resource scan(String root, File directory) throws IOException {
    File props = new File(directory, RESOURCE_FILE_NAME);
    if(!props.exists()){
      return null;
    }
    try (FileInputStream fis = new FileInputStream(props)) {
      Yaml yaml = new Yaml();
      ResourceProperties properties = yaml.load(fis);
      String name = properties.getResourceName();
      String uuidProp = properties.getUUID();
      if (name != null && uuidProp != null) {
        int idx = uuidProp.indexOf(':');
        if (idx != -1) {
          String mostString = uuidProp.substring(0, idx);
          String leastString = uuidProp.substring(idx + 1);
          long least = Long.parseLong(leastString);
          long most = Long.parseLong(mostString);
          UUID uuid = new UUID(most, least);
          return createPersistentResource(root, name, uuid);
        }
      }
    }
    Files.delete(directory.toPath());
    return null;
  }

  private Resource createPersistentResource(String path, String resourceName, UUID uuid) throws IOException {
    String directoryPath = path + File.separator + uuid.toString() + File.separator;
    switch(type){
      case DATABASE:
        return new DBResource(directoryPath, resourceName);

      case RANDOM_ACCESS_FILE:
        return new FileResource(directoryPath, resourceName);

      case MEMORY:
        return new MemoryResource(resourceName);

      case CHANNEL:
      default:
        return new SeekableChannelResource(directoryPath, resourceName);
    }
  }

  private void createMetaData(String path, String resourceName, UUID uuid, DestinationType destinationType) throws IOException {
    File directoryPath = new File(path + File.separator + uuid.toString() + File.separator);
    if (!directoryPath.exists()) {
      if (!directoryPath.mkdirs()) {
        throw new IOException("Unable to construct directory path " + directoryPath.toString());
      }
      ResourceProperties properties = new ResourceProperties(
          new Date(),
          resourceName,
          destinationType.getName(),
          uuid.getMostSignificantBits() + ":" + uuid.getLeastSignificantBits(),
          BuildInfo.getInstance().getBuildDate(),
          BuildInfo.getInstance().getBuildVersion()
      );
      final DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      options.setPrettyFlow(true);
      final Yaml yaml = new Yaml(options);
      FileWriter writer = new FileWriter(directoryPath + File.separator + RESOURCE_FILE_NAME);
      yaml.dump(properties, writer);
      writer.close();
    }
  }
}
