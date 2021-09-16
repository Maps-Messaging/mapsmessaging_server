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

import io.mapsmessaging.storage.StorageFactoryFactory;
import io.mapsmessaging.storage.impl.layered.weakReference.WeakReferenceCacheStorage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class FileResource extends MapBasedResource {

  public FileResource(String name, String mapped) throws IOException {
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
    setStore(new WeakReferenceCacheStorage<>(Objects.requireNonNull(StorageFactoryFactory.getInstance().create("File", properties, new MessageFactory())).create(name)));
  }


}
