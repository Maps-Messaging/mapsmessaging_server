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

package io.mapsmessaging.network;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class EndPointURL {

  @Getter
  protected String host;
  @Getter
  protected int port;
  @Getter
  protected String protocol;
  @Getter
  protected String file;
  protected Map<String, String> parameterMap;

  protected EndPointURL() {
  }

  public EndPointURL(String url) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("URL must not be null or empty");
    }
    int hostStart = processProtocol(url);

    if (hostStart >= url.length()) {
      throw new IllegalArgumentException("URL must contain host");
    }

    int pathStart = url.indexOf('/', hostStart);
    int hostEnd = (pathStart == -1) ? url.length() : pathStart;

    String hostPort = url.substring(hostStart, hostEnd);
    if (hostPort.isEmpty()) {
      throw new IllegalArgumentException("URL must contain host");
    }
    String portPart = processHost(hostPort);
    processPort(portPart);
    processFile(url, pathStart);
    parseParameterMap(url);
  }

  private int processProtocol(String url){
    int schemeEnd = url.indexOf("://");
    int hostStart;

    if (schemeEnd > 0) {
      protocol = url.substring(0, schemeEnd);
      hostStart = schemeEnd + 3;
    } else {
      schemeEnd = url.indexOf(":/");
      if (schemeEnd <= 0) {
        throw new IllegalArgumentException("URL must contain protocol and separator");
      }
      protocol = url.substring(0, schemeEnd);
      hostStart = schemeEnd + 2;
    }
    return hostStart;
  }

  private String processHost(String hostPort){

    String hostPart;
    String portPart = null;

    // Special-case: tcp://:::443 or tcp:/:::443  => host "::", port 443
    if (hostPort.startsWith(":::")) {
      int lastColon = hostPort.lastIndexOf(':');
      String possiblePort = hostPort.substring(lastColon + 1);
      if (!possiblePort.isEmpty() && isAllDigits(possiblePort)) {
        hostPart = "::";
        portPart = possiblePort;
      } else {
        hostPart = hostPort;
      }
    }
    // Bracketed IPv6: [2001:db8::1]:443, [::1], [fe80::1%eth0]:1234
    else if (hostPort.startsWith("[")) {
      int closingBracket = hostPort.indexOf(']');
      if (closingBracket == -1) {
        throw new IllegalArgumentException("Invalid IPv6 literal: missing ']'");
      }
      hostPart = hostPort.substring(1, closingBracket);
      if (closingBracket + 1 < hostPort.length() && hostPort.charAt(closingBracket + 1) == ':') {
        portPart = hostPort.substring(closingBracket + 2);
      }
    }
    // Non-bracketed: decide IPv4/hostname vs IPv6-without-port
    else {
      int colonCount = 0;
      for (int i = 0; i < hostPort.length(); i++) {
        if (hostPort.charAt(i) == ':') {
          colonCount++;
        }
      }

      if (colonCount > 1) {
        // Treat as pure IPv6 host literal, no port
        hostPart = hostPort;
      } else {
        int colonIndex = hostPort.indexOf(':');
        if (colonIndex >= 0) {
          hostPart = hostPort.substring(0, colonIndex);
          portPart = hostPort.substring(colonIndex + 1);
        } else {
          hostPart = hostPort;
        }
      }
    }

    host = hostPart;
    return portPart;
  }

  private void processPort(String portPart){
    if (portPart != null && !portPart.isEmpty()) {
      try {
        port = Integer.parseInt(portPart);
      } catch (NumberFormatException e) {
        port = 0;
      }
    } else {
      port = 0;
    }
  }
  private void processFile(String url, int pathStart){
    if (pathStart == -1) {
      file = "";
    } else {
      file = url.substring(pathStart + 1);
      int query  = file.indexOf('?');
      if (query >= 0) {
        file = file.substring(0, query);
      }
    }
  }


  private boolean isAllDigits(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isDigit(value.charAt(i))) {
        return false;
      }
    }
    return !value.isEmpty();
  }


  public Map<String, String> getParameters() {
    return parameterMap;
  }


  public String getJMXName() {
    if(getPort() == 0){
      return getProtocol() + "_" + getHost();
    }
    return getProtocol() + "_" + getHost() + "_" + getPort();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(protocol).append("://").append(host);
    if(port != 0){
      builder.append(":").append(port);
    }
    if(file != null){
      builder.append("/").append(file);
    }
    if(parameterMap != null && !parameterMap.isEmpty()){
      builder.append("?");
      boolean first = true;
      for(Map.Entry<String, String> entry : parameterMap.entrySet()) {
        if(!first){
          builder.append("&");
        }
        first = false;
        builder.append(entry.getKey()).append("=").append(entry.getValue());
      }
    }
    return builder.toString();
  }

  protected String parseProtocol(String url) {
    int idx = url.indexOf("://");
    return url.substring(0, idx);
  }

  protected void parseParameterMap(String url) {
    parameterMap = new LinkedHashMap<>();
    int paramIdx = url.indexOf('?');
    if (paramIdx != -1) {
      String parameters = url.substring(paramIdx + 1);
      StringTokenizer st = new StringTokenizer(parameters, "&");
      while (st.hasMoreElements()) {
        String param = st.nextElement().toString();
        int idx = param.indexOf('=');
        if (idx != -1) {
          String key = param.substring(0, idx).toLowerCase();
          String val = param.substring(idx + 1);
          parameterMap.put(key, val);
        }
      }
    }
  }

  protected String parseHost(String url) {
    int startIdx = url.indexOf("://") + 3;
    String tmp = url.substring(startIdx);
    int endIdx = tmp.lastIndexOf(':');
    if (endIdx > 0) {
      return tmp.substring(0, endIdx);
    }
    endIdx = tmp.indexOf('/');
    if (endIdx != -1) {
      return tmp.substring(0, endIdx);
    }
    return tmp;
  }

  protected int parsePort(String port) {
    String val = port;
    int idx = val.indexOf('/');
    if (idx > 0) {
      val = val.substring(0, idx);
    }
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException e) {
      return 0;
    }
  }



}
