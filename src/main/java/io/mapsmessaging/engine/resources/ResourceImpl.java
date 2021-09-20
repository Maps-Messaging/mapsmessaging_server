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

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.storage.StorageBuilder;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceImpl extends Resource {

  public ResourceImpl(String name, String mapped, String type) throws IOException {
    super(name, mapped);
    String tmpName = name;
    if (File.separatorChar == '/') {
      while (tmpName.indexOf('\\') != -1) {
        tmpName = tmpName.replace("\\", File.separator);
      }
    } else {
      while (tmpName.indexOf('/') != -1) {
        tmpName = tmpName.replace("/", File.separator);
      }
    }
    tmpName += "data.bin";
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("basePath", tmpName);
    tmpName += "data.bin";

    StorageBuilder<Message> builder = new StorageBuilder<>();
    builder.setCache()
        .setProperties(properties)
        .setName(tmpName)
        .setFactory(new MessageFactory())
        .setStorageType(type);
    setStore(builder.build());
    persistent = !(type.equals("Memory"));
  }


}
