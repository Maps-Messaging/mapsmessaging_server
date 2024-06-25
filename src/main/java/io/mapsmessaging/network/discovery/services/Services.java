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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

public class Services {

  @Getter
  private final String protocol;
  @Getter
  private final int port;
  @Getter
  private final String transport;

  private final List<String> addresses;

  public Services(MapsServiceInfo serviceInfo) {
    protocol = serviceInfo.getApplication();
    port = serviceInfo.getPort();
    transport = serviceInfo.getProtocol();
    addresses = new ArrayList<>();
    addresses.addAll(Arrays.asList(serviceInfo.getHostAddresses()));
  }

  public void mergeServices(MapsServiceInfo serviceInfo){
    for(String address : serviceInfo.getHostAddresses()){
      if(!addresses.contains(address)){
        addresses.add(address);
      }
    }
  }

  public List<String> getAddresses(){
    return Collections.unmodifiableList(addresses);
  }

  public String toString(){
    String header = protocol + ":" + port;

    return header + "\t\t[" + String.join(",", addresses)+"]";
  }

  public void removeService(MapsServiceInfo serviceInfo) {
    for(String address : serviceInfo.getHostAddresses()){
      addresses.remove(address);
    }
  }
}
