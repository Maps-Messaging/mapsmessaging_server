/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.DestinationPathManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.UUID;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ResourceFactory {
  public static final String RESOURCE_FILE_NAME = "resource.yaml";

  private static final ResourceFactory instance = new ResourceFactory();


  private ResourceFactory() {
  }

  public static ResourceFactory getInstance() {
    return instance;
  }

  public Resource create(MessageExpiryHandler messageExpiryHandler, String resourceName, DestinationPathManager pathManager, String fullyQualifiedPath, UUID uuid, DestinationType destinationType) throws IOException {
    if (resourceName.toLowerCase().startsWith("$sys")) {
      return new Resource();
    } else {
      createMetaData(pathManager, resourceName, uuid, destinationType);
      return new Resource(messageExpiryHandler, pathManager, fullyQualifiedPath);
    }
  }

  public ResourceProperties scanForProperties(File directory) throws IOException {
    File props = new File(directory, RESOURCE_FILE_NAME);
    if(!props.exists()){
      return null;
    }
    try (FileInputStream fis = new FileInputStream(props)) {
      Yaml yaml = new Yaml();
      return yaml.load(fis);
    }
  }

  public Resource scan(MessageExpiryHandler messageExpiryHandler, File directory, DestinationPathManager pathManager, ResourceProperties properties) throws IOException {
    String name = properties.getResourceName();
    String uuidProp = properties.getUuid();
    if (name != null && uuidProp != null) {
      int idx = uuidProp.indexOf(':');
      if (idx != -1) {
        String mostString = uuidProp.substring(0, idx);
        String leastString = uuidProp.substring(idx + 1);
        long least = Long.parseLong(leastString);
        long most = Long.parseLong(mostString);
        UUID uuid = new UUID(most, least);
        String fullyQualifiedPath = pathManager.getDirectory() + File.separator + uuid.toString() + File.separator;
        return new Resource(messageExpiryHandler, pathManager, fullyQualifiedPath);
      }
    }
    Files.delete(directory.toPath());
    return null;
  }

  private void createMetaData(DestinationPathManager path, String resourceName, UUID uuid, DestinationType destinationType) throws IOException {
    File directoryPath = new File(path.getDirectory() + File.separator + uuid.toString() + File.separator);
    if (!directoryPath.exists()) {
      if (!directoryPath.mkdirs()) {
        throw new IOException("Unable to construct directory path " + directoryPath);
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
