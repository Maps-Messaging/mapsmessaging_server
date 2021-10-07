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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import lombok.Getter;

public class DestinationPathManager {

  private static final String OPTIONAL_PATH = "{folder}";

  private final @Getter String name;
  private final @Getter String directory;
  private final @Getter String namespace;
  private final @Getter String type;
  private final @Getter boolean enableSync;
  private final @Getter boolean remap;
  private final @Getter int itemCount;
  private final @Getter int expiredEventPoll;
  private final @Getter long partitionSize;

  private final @Getter String cacheType;
  private final @Getter boolean writeThrough;
  private final @Getter boolean enableCache;


  public DestinationPathManager(ConfigurationProperties properties){
    name = properties.getProperty("name");
    type = properties.getProperty("type", "File");

    String propertyNamespace = properties.getProperty("namespace" );
    String tmp = properties.getProperty("directory");
    if(tmp == null){
      tmp = "";
    }
    directory = tmp;
    remap = (propertyNamespace.endsWith(OPTIONAL_PATH) && directory.contains(OPTIONAL_PATH));
    if(remap){
      namespace = propertyNamespace.substring(0, propertyNamespace.indexOf(OPTIONAL_PATH));
    }
    else{
      namespace = propertyNamespace;
    }
    enableSync = properties.getBooleanProperty("sync", false);
    itemCount = properties.getIntProperty("itemCount", 524288);
    partitionSize = properties.getLongProperty("maxPartitionSize", 4_294_967_296L);

    expiredEventPoll = properties.getIntProperty("expiredEventPoll", 1);

    if(properties.containsKey("cache")){
      ConfigurationProperties cacheProps = (ConfigurationProperties)properties.get("cache");
      enableCache = true;
      cacheType = cacheProps.getProperty("type", "WeakReference");
      writeThrough = cacheProps.getBooleanProperty("writeThrough", false);
    }
    else{
      cacheType = "";
      writeThrough = false;
      enableCache = false;
    }
  }

  public String getRootDirectory(){
    if(remap){
      return directory.substring(0, directory.indexOf(OPTIONAL_PATH));
    }
    return directory;
  }

  public String calculateDirectory(String destinationPath){
    if(remap){
      String response;
      String tmp = destinationPath.substring(namespace.length());
      if(tmp.contains("/")){
        String sub = tmp.substring(0, tmp.indexOf("/"));
        response = directory.replace(OPTIONAL_PATH, sub);
      }
      else {
        response = directory.replace(OPTIONAL_PATH, tmp);
      }
      return response;
    }
    return directory;
  }

  public String getTrailingPath() {
    if(remap){
      return directory.substring(directory.indexOf(OPTIONAL_PATH)+OPTIONAL_PATH.length());
    }
    return "";
  }

}
