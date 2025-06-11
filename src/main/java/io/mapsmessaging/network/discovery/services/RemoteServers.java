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

package io.mapsmessaging.network.discovery.services;

import io.mapsmessaging.dto.rest.discovery.DiscoveredServersDTO;
import io.mapsmessaging.network.discovery.MapsServiceInfo;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class RemoteServers extends DiscoveredServersDTO implements Serializable {

  public RemoteServers(MapsServiceInfo serviceInfo) {
    this.serverName = serviceInfo.getServerName();
    this.schemaPrefix = serviceInfo.getSchemaPrefix();
    this.schemaSupport = serviceInfo.supportsSchema();
    this.systemTopicPrefix = serviceInfo.getSystemTopicPrefix();
    this.buildDate = serviceInfo.getBuildDate();
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

  public String toString(){
    StringBuilder header = new StringBuilder(serverName);
    header.append(" Version:").append(version).append(" Schema Prefix:").append(schemaPrefix).append(" Topic Prefix:").append(systemTopicPrefix);
    for(Services service : services.values()){
      header.append("\n\t").append(service.toString());
    }
    header.append("\n\n");
    return header.toString();
  }
}
