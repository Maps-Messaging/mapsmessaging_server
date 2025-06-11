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

import lombok.Data;
import lombok.ToString;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@Data
public class ResourceProperties {

  public static final String RESOURCE_FILE_NAME = "resource.yaml";

  private Date date;
  private String resourceName;
  private String type;
  private String uuid;
  private String buildDate;
  private String buildVersion;
  private Map<String, Object> schema;

  public ResourceProperties() {}

  public ResourceProperties(Date date, String resourceName, String type, String uuid, String buildDate, String buildVersion) {
    this.date = date;
    this.resourceName = resourceName;
    this.type = type;
    this.uuid = uuid;
    this.buildDate = buildDate;
    this.buildVersion = buildVersion;
    schema = new LinkedHashMap<>();
  }

  public void write(File directoryPath) throws IOException {
    final DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    final Yaml yaml = new Yaml(options);
    FileWriter writer = new FileWriter(directoryPath.getAbsolutePath() + File.separator + RESOURCE_FILE_NAME);
    yaml.dump(this, writer);
    writer.close();
  }
}
