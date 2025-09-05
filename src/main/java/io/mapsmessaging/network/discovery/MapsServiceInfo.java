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

package io.mapsmessaging.network.discovery;

import lombok.Getter;
import lombok.ToString;

import javax.jmdns.ServiceInfo;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

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


  public String getBuildDate() {
    return getProperty("date");
  }

  public String getServerName(){
    String val = getProperty("server name");
    if(val.isEmpty()){
      val = serviceInfo.getName();
      if(val.contains("(")){
        val = val.substring(0, val.indexOf("(")).trim();
      }
    }
    return val;
  }

  public String getSystemTopicPrefix(){
    return getProperty("system topics");
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

  public Map<String, String> getProperties() {
    Map<String, String> props = new LinkedHashMap<>();
    Enumeration<String> enumeration = getPropertyNames();
    while(enumeration.hasMoreElements()){
      String key = enumeration.nextElement();
      props.put(key, getProperty(key));
    }
    props.remove("protocol");
    props.remove("system topics");
    props.remove("schema name");
    props.remove("schema support");
    props.remove("server name");
    props.remove("version");
    props.remove("date");
    return props;
  }

}
