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

package io.mapsmessaging.app.top.network;

import com.google.gson.*;
import io.mapsmessaging.utilities.GsonFactory;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

public abstract class RestApiConnection {
  protected final Gson gson;
  protected final String url;
  protected final String endpoint;

  protected RestApiConnection(String url, String endpoint) {
    this.url = url;
    this.endpoint = endpoint;
    this.gson = GsonFactory.getInstance().getPrettyGson();
  }

  public abstract Object parse(JsonElement jsonElement) throws JsonParseException;

  public Object getData() throws IOException {
    RestRequestManager manager = RestRequestManager.getInstance();
    manager.ensureValidSession();

    WebTarget target = manager.getClient().target(url).path(endpoint);
    Response response = target.request(MediaType.APPLICATION_JSON).get();

    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
      String jsonString = response.readEntity(String.class);
      JsonElement jsonElement;

      if (jsonString.trim().startsWith("[")) {
        JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("data", jsonArray);
        jsonElement = jsonObject;
      } else {
        jsonElement = JsonParser.parseString(jsonString);
      }
      return parse(jsonElement);
    } else if (response.getStatus() == Response.Status.FORBIDDEN.getStatusCode()) {
      throw new IOException("Access denied");
    } else {
      throw new IOException("Unexpected error: " + response.getStatus());
    }
  }

}
