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

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.schemas.config.SchemaConfig;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.UUID;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class ResourceFactory {


  private static class Holder {
    static final ResourceFactory INSTANCE = new ResourceFactory();
  }

  public static ResourceFactory getInstance() {
    return Holder.INSTANCE;
  }


  public static final String RESOURCE_FILE_NAME = "resource.yaml";

  private ResourceFactory() {
  }

  public Resource create(MessageExpiryHandler messageExpiryHandler, String resourceName, DestinationConfigDTO pathManager, String fullyQualifiedPath, UUID uuid,
                         DestinationType destinationType, SchemaConfig config) throws IOException {
    if (resourceName.toLowerCase().startsWith("$sys")) {
      return new ResourceImpl();
    } else {
      ResourceProperties props = createMetaData(pathManager.getDirectory() , resourceName, uuid, destinationType, config);
      return new ResourceImpl(messageExpiryHandler, pathManager, fullyQualifiedPath, props);
    }
  }

  public ResourceProperties scanForProperties(File directory) throws IOException {
    File props = new File(directory, RESOURCE_FILE_NAME);
    if (!props.exists()) {
      return null;
    }
    try (FileInputStream fis = new FileInputStream(props)) {
      var loaderoptions = new LoaderOptions();
      TagInspector taginspector = tag -> tag.getClassName().equals(io.mapsmessaging.engine.resources.ResourceProperties.class.getName());
      loaderoptions.setTagInspector(taginspector);
      Yaml yaml = new Yaml(new Constructor(io.mapsmessaging.engine.resources.ResourceProperties.class, loaderoptions));
      return yaml.load(fis);
    }
  }

  public Resource scan(MessageExpiryHandler messageExpiryHandler, File directory, DestinationConfigDTO pathManager, ResourceProperties properties) throws IOException {
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
        String fullyQualifiedPath = pathManager.getDirectory() + File.separator + uuid + File.separator;
        return new ResourceImpl(messageExpiryHandler, pathManager, fullyQualifiedPath, properties);
      }
    }
    Files.delete(directory.toPath());
    return null;
  }

  private ResourceProperties createMetaData(String path, String resourceName, UUID uuid, DestinationType destinationType, SchemaConfig config) throws IOException {
    File directoryPath = new File(path + File.separator + uuid.toString() + File.separator);
    if (!directoryPath.exists()) {
      if (!directoryPath.mkdirs()) {
        throw new IOException("Unable to construct directory path " + directoryPath);
      }
      ResourceProperties properties = new ResourceProperties(
          new Date(),
          resourceName,
          destinationType.getName(),
          uuid.getMostSignificantBits() + ":" + uuid.getLeastSignificantBits(),
          BuildInfo.getBuildDate(),
          BuildInfo.getBuildVersion()
      );
      if(config != null) {
        properties.setSchemaId(config.getUniqueId());
      }
      properties.write(directoryPath);
      return properties;
    }
    return null;
  }
}
