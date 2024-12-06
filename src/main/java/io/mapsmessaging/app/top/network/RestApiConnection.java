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

package io.mapsmessaging.app.top.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public abstract class  RestApiConnection {
  protected final ObjectMapper mapper;
  protected final String url;
  protected final String endpoint;

  protected RestApiConnection(String url, String endpoint, String username, String password) {
    this.url = url;
    this.endpoint = endpoint;
    mapper = new ObjectMapper();
  }

  public abstract Object parse(JSONObject jsonObject) throws JsonProcessingException, JSONException;

  public Object getData() throws IOException, JSONException {
    Client client = ClientBuilder.newClient();
    try {
      WebTarget target = client.target(url).path(endpoint);
      Response response = target.request(MediaType.APPLICATION_JSON).get();

      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        String jsonString =  response.readEntity(String.class);
        JSONObject jsonObject;
        if(jsonString.startsWith("[")){
          JSONArray jsonArray = new JSONArray(jsonString);
          jsonObject = new JSONObject();
          jsonObject.put("data", jsonArray);
        } else {
          jsonObject = new JSONObject(jsonString);
        }
        return parse(jsonObject);
      } else if (response.getStatus() == Response.Status.FORBIDDEN.getStatusCode()) {
        throw new IOException("Access denied");
      } else {
        throw new IOException("Unexpected error: " + response.getStatus());
      }
    } finally {
      client.close();
    }
  }


}
