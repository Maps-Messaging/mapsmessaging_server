/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.discovery.services;

import io.mapsmessaging.network.discovery.MapsServiceInfo;
import java.util.*;
import lombok.Getter;

public class RemoteServers {

  @Getter
  private final String serverName;
  @Getter
  private final boolean schemaSupport;
  @Getter
  private final String schemaPrefix;
  @Getter
  private final String version;

  private final Map<String, Services> services;

  public RemoteServers(MapsServiceInfo serviceInfo) {
    this.serverName = serviceInfo.getServerName();
    this.schemaPrefix = serviceInfo.getSchemaPrefix();
    this.schemaSupport = serviceInfo.supportsSchema();
    this.version = serviceInfo.getVersion();
    services = new LinkedHashMap<>();
    Services service = new Services(serviceInfo);
    services.put(service.getProtocol(), service);
  }

  public void update(MapsServiceInfo serviceInfo){
    Services service = services.get(serviceInfo.getApplication());
    if(service != null){
      service.mergeServices(serviceInfo);
    }
    else{
      service = new Services(serviceInfo);
      services.put(service.getProtocol(), service);
    }
  }

  public List<Services> getServices(){
    return Collections.unmodifiableList(new ArrayList<>(services.values()));
  }


  public String toString(){
    String header = serverName +" Schema Prefix:"+schemaPrefix+" Version:"+version;
    for(Services service : services.values()){
      header += "\n\t"+service.toString();
    }
    header+="\n\n";
    return header;

  }

  public void remove(MapsServiceInfo serviceInfo) {
    Services service = services.get(serviceInfo.getApplication());
    if(service != null){
      service.removeService(serviceInfo);
      if(service.getAddresses().isEmpty()){
        services.remove(serviceInfo.getApplication());
      }
    }
  }
}
