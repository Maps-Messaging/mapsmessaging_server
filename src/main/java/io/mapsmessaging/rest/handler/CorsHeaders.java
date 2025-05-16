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

package io.mapsmessaging.rest.handler;

import jakarta.ws.rs.container.ContainerResponseContext;
import lombok.Data;
import org.glassfish.grizzly.http.server.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class CorsHeaders {

  private final Map<String, String> headers;

  public CorsHeaders() {
    headers = new ConcurrentHashMap<>();
  }

  public void addHeader(String header, String value) {
    headers.put(header, value);
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
