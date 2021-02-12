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

package org.maps.messaging.engine.destination;

import org.maps.utilities.configuration.ConfigurationProperties;

public class DestinationPathManager {

  private static final String NAME = "name";
  private static final String NAMESPACE = "namespace";
  private static final String DIRECTORY = "directory";

  private static final String OPTIONAL_PATH = "{folder}";

  private final String name;
  private final String directory;
  private final String namespace;
  private final boolean remap;

  public DestinationPathManager(ConfigurationProperties properties){
    String nam = properties.getProperty(NAMESPACE);
    String tmp = properties.getProperty(DIRECTORY);
    if(tmp == null){
      tmp = "";
    }
    directory = tmp;
    name = properties.getProperty(NAME);
    remap = (nam.endsWith(OPTIONAL_PATH) && directory.contains(OPTIONAL_PATH));
    if(remap){
      namespace = nam.substring(0, nam.indexOf(OPTIONAL_PATH));
    }
    else{
      namespace = nam;
    }
  }

  public String getName() {
    return name;
  }

  public boolean isRemap() {
    return remap;
  }

  public String getDirectory() {
    return directory;
  }

  public String getRootDirectory(){
    if(remap){
      return directory.substring(0, directory.indexOf(OPTIONAL_PATH));
    }
    return directory;
  }

  public String getNamespace() {
    return namespace;
  }

  public String calculateDirectory(String destinationPath){
    if(remap){
      String response = "";
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
