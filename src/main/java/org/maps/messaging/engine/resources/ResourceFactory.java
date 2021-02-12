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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import org.maps.messaging.BuildInfo;
import org.maps.messaging.api.features.DestinationType;

public class ResourceFactory {

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
      return createPersistentResource(path, resourceName, uuid, destinationType);
    }
  }

  public Resource scan(String root, File directory) throws IOException {
    Properties properties = new Properties();
    File props = new File(directory, "resource.props");
    if(!props.exists()){
      Files.delete(directory.toPath());
      return null;
    }
    try (FileInputStream fis = new FileInputStream(props)) {
      properties.load(fis);
      String name = properties.getProperty("resourceName");
      String uuidProp = properties.getProperty("UUID");
      DestinationType destinationType = DestinationType.getType(properties.getProperty("type"));
      if (name != null && uuidProp != null) {
        int idx = uuidProp.indexOf(':');
        if (idx != -1) {
          String mostString = uuidProp.substring(0, idx);
          String leastString = uuidProp.substring(idx + 1);
          long least = Long.parseLong(leastString);
          long most = Long.parseLong(mostString);
          UUID uuid = new UUID(most, least);
          return createPersistentResource(root, name, uuid, destinationType);
        }
      }
    }
    Files.delete(directory.toPath());
    return null;
  }

  private Resource createPersistentResource(String path, String resourceName, UUID uuid, DestinationType destinationType) throws IOException {
    createMetaData(path, resourceName, uuid, destinationType);
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
      Properties properties = new Properties();
      Date dt = new Date();
      properties.put("created", dt.toString());
      properties.put("resourceName", resourceName);
      properties.put("type", destinationType.getName());
      properties.put("UUID", uuid.getMostSignificantBits() + ":" + uuid.getLeastSignificantBits());
      properties.put("BuildDate", BuildInfo.getInstance().getBuildDate());
      properties.put("BuildVersion", BuildInfo.getInstance().getBuildVersion());

      File props = new File(directoryPath, "resource.props");
      try (FileOutputStream fos = new FileOutputStream(props)) {
        properties.store(fos, "Auto created, please do not edit, may cause instabilities");
        fos.flush();
      }
    }
  }
}
