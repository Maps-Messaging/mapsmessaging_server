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
import java.io.Serializable;
import java.util.*;
import lombok.Getter;

@Getter
public class RemoteServers implements Serializable {

  private final String serverName;
  private final String systemTopics;
  private final boolean schemaSupport;
  private final String schemaPrefix;
  private final String version;
  private final Map<String, Services> services;

  public RemoteServers(MapsServiceInfo serviceInfo) {
    this.serverName = serviceInfo.getServerName();
    this.schemaPrefix = serviceInfo.getSchemaPrefix();
    this.schemaSupport = serviceInfo.supportsSchema();
    this.systemTopics = serviceInfo.getSystemTopicPrefix();
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

  public void remove(MapsServiceInfo serviceInfo) {
    services.remove(serviceInfo.getApplication());
  }

  public List<Services> getServices(){
    return Collections.unmodifiableList(new ArrayList<>(services.values()));
  }

  public String toString(){
    String header = serverName +" Version:"+version +" Schema Prefix:"+schemaPrefix+" Topic Prefix:"+systemTopics;
    for(Services service : services.values()){
      header += "\n\t"+service.toString();
    }
    header+="\n\n";
    return header;

  }
}
