/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.consul;

import lombok.Data;
import lombok.ToString;

import java.net.MalformedURLException;
import java.net.URL;

@ToString
@Data
public class ConsulConfiguration {

  private final String consulToken;
  private final String consulUrl;
  private final String urlPath;
  private final String consulAcl;

  public ConsulConfiguration() {

    String tokencfg;
    String path = "/";

    String urlCfg = System.getProperty("ConsulUrl");
    if (urlCfg != null) {
      tokencfg = extractToken(urlCfg);
      path = extractPath(urlCfg);
      if (tokencfg != null) {
        urlCfg = removeToken(urlCfg);
      }
      urlCfg = removePath(urlCfg);
    } else {
      tokencfg = null;
      String host = System.getProperty("ConsulHost", "127.0.0.1");
      int port = Integer.parseInt(System.getProperty("ConsulPort", "8500"));
      urlCfg = "http://" + host + ":" + port;
    }

    consulAcl = System.getProperty("ConsulAcl");
    consulToken = parseToken(tokencfg);
    consulUrl = urlCfg;
    urlPath = path;
  }

  public boolean registerAgent() {
    return Boolean.parseBoolean(System.getProperty("ConsulAgentRegister", "false"));
  }

  private String parseToken(String tokencfg) {
    String tokenProp = System.getProperty("ConsulToken", tokencfg);
    if (tokenProp != null && !tokenProp.isEmpty()) {
      tokencfg = tokenProp;
    }
    return (tokencfg != null && !tokencfg.trim().isEmpty()) ? tokencfg.trim() : null;
  }

  private String removePath(String urlString) {
    try {
      URL url = new URL(urlString);
      return url.getPort() != -1 ? url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() : url.getProtocol() + "://" + url.getHost();
    } catch (MalformedURLException e) {
      // ignore
    }
    return urlString;
  }

  private String removeToken(String url) {
    if (url.contains("@")) {
      int tokenEnd = url.indexOf("@");
      int tokenStart = url.indexOf("://");
      if (tokenStart < tokenEnd) {
        url = url.substring(0, tokenStart + 3) + url.substring(tokenEnd + 1);
      }
    }
    return url;
  }

  private String extractPath(String urlString) {
    try {
      URL url = new URL(urlString);
      String path = url.getPath().trim();
      if (path.isEmpty()) {
        path = "/";
      }
      return path;
    } catch (MalformedURLException e) {
      // ignore
    }
    return "/";
  }

  private String extractToken(String url) {
    String token = null;
    if (url.contains("@")) {
      int tokenEnd = url.indexOf("@");
      int tokenStart = url.indexOf("://");
      if (tokenStart < tokenEnd) {
        token = url.substring(tokenStart + 3, tokenEnd).trim();
        if (token.startsWith(":")) {
          token = token.substring(1);
        }
      }
    }
    return token;
  }
}
