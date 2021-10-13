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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class SessionTenantConfig {

  private final @Getter String tenantPath;
  private final List<String> globalList; // Should be a tree

  public SessionTenantConfig(String tenantPath, List<ConfigurationProperties> globalConfiguration){
    if(tenantPath.equals("/")){
      tenantPath = "";
    }
    this.tenantPath = tenantPath;
    globalList = new ArrayList<>();
    if(globalConfiguration != null) {
      for (ConfigurationProperties config : globalConfiguration) {
        String namespace = config.getProperty("namespaceRoot");
        if(namespace != null && namespace.length() > 1){
          globalList.add(namespace);
        }
      }
    }
  }

  public String calculateOriginalName(String fqn){
    if(fqn.startsWith("$SYS") || isGlobal(fqn)){ // This is a global common path used by all users
      return fqn; // Keep the name as it is
    }
    String name = fqn.substring(tenantPath.length());
    if(name.startsWith("_")){
      name = "/"+name.substring(1);
    }
    return name;
  }

  // Prepends the tenant path to the destination name
  public String calculateDestinationName(String destinationName){
    if(destinationName.startsWith("$SYS") || isGlobal(destinationName)){ // This is a global common path used by all users
      return destinationName;
    }
    if(tenantPath.length() > 0) {
      if (destinationName.startsWith("/")) {
        destinationName = "_" + destinationName.substring(1);
      }
    }
    return tenantPath +destinationName;
  }


  private boolean isGlobal(String destinationName){
    for(String test:globalList){
      if(destinationName.startsWith(test)){
        return true;
      }
    }
    return false;
  }

}
