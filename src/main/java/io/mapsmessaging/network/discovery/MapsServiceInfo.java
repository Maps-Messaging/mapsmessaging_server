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

package io.mapsmessaging.network.discovery;

import java.util.Enumeration;
import javax.jmdns.ServiceInfo;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MapsServiceInfo {

  private final ServiceInfo serviceInfo;

  public MapsServiceInfo(final ServiceInfo serviceInfo) {
    this.serviceInfo = serviceInfo;
  }

  public String getProtocol(){
    return getProperty("protocol");
  }

  public boolean supportsSchema(){
    return Boolean.parseBoolean(serviceInfo.getPropertyString("schema support"));
  }

  public String getSchemaPrefix(){
    return getProperty("schema name");
  }

  public String getServerName(){
    return getProperty("server name");
  }

  public String getPropertyString(String serverName) {
    return getProperty(serverName);
  }

  public Enumeration<String> getPropertyNames() {
    return serviceInfo.getPropertyNames();
  }

  public String getName() {
    return serviceInfo.getName();
  }

  public String getType() {
    return serviceInfo.getType();
  }

  public String[] getHostAddresses() {
    return serviceInfo.getHostAddresses();
  }

  public String getDomain() {
    return serviceInfo.getDomain();
  }

  public int getPort() {
    return serviceInfo.getPort();
  }

  public String getApplication() {
    return serviceInfo.getApplication();
  }

  public String getVersion() {
    return getProperty("version");
  }

  private String getProperty(String name){
    String val = serviceInfo.getPropertyString(name);
    if(val == null){
      val = "";
    }
    return val;
  }

}
