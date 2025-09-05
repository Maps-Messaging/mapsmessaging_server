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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class EndPointURL {

  protected String host;
  protected int port;
  protected String protocol;
  protected String file;
  protected Map<String, String> parameterMap;

  protected EndPointURL() {
  }

  public EndPointURL(String url) {
    protocol = parseProtocol(url);
    host = parseHost(url);
    String tmp = protocol + "://" + host + "/";
    port = parsePort(url.substring(tmp.length()));
    tmp = protocol + "://" + host + ":" + port + "/";
    if (url.length() > tmp.length()) {
      file = url.substring(tmp.length());
    } else {
      file = "";
    }
    parseParameterMap(url);
  }

  public Map<String, String> getParameters() {
    return parameterMap;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getFile() {
    return file;
  }


  public String getJMXName() {
    return getProtocol() + "_" + getHost() + "_" + getPort();
  }

  @Override
  public String toString() {
    if (file != null) {
      return protocol + "://" + host + ":" + port + "/" + file;
    }
    return protocol + "://" + host + ":" + port + "/";
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
    endIdx = tmp.indexOf('/', startIdx + 1);
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
