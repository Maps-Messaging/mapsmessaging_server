/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.rest.handler;

import jakarta.ws.rs.container.ContainerResponseContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.glassfish.grizzly.http.server.Response;

public class CorsHeaders {

  private final Map<String, String> headers;

  public CorsHeaders() {
    headers = new ConcurrentHashMap<>();
    headers.put("Access-Control-Allow-Origin", "*"); // Use "*" or specific domain
    headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
    headers.put("Access-Control-Allow-Credentials", "true"); // Include if using cookies or authentication
  }


  public void updateResponseHeaders(Response response) {
    for(Map.Entry<String, String> header:headers.entrySet()) {
      response.addHeader(header.getKey(), header.getValue());
    }
  }

  public void addFilter(ContainerResponseContext responseContext) {
    for(Map.Entry<String, String> header:headers.entrySet()) {
      responseContext.getHeaders().add(header.getKey(), header.getValue());
    }
  }
}
