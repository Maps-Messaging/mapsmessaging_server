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

import io.mapsmessaging.network.discovery.MapsServiceInfo;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Getter
public class Services {

  private final String protocol;
  private final int port;
  private final String transport;
  private final List<String> addresses;
  private final Map<String, String> properties;

  public Services(MapsServiceInfo serviceInfo) {
    protocol = serviceInfo.getApplication();
    port = serviceInfo.getPort();
    transport = serviceInfo.getProtocol();
    addresses = new ArrayList<>();
    addresses.addAll(Arrays.asList(serviceInfo.getHostAddresses()));
    properties = serviceInfo.getProperties();
  }

  public void mergeServices(MapsServiceInfo serviceInfo){
    for(String address : serviceInfo.getHostAddresses()){
      if(!addresses.contains(address)){
        addresses.add(address);
      }
    }
  }

  public String toString(){
    StringBuilder header = new StringBuilder(protocol).append( ":").append(port);

    header.append("\t\t[").append(String.join(",", addresses)).append("]");
    for(Map.Entry<String, String> entry : properties.entrySet()){
      header.append("\n\t\t\t[").append(entry.getKey()).append("=").append(entry.getValue()).append("]");
    }
    return header.toString();
  }

}
