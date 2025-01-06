/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

import com.google.gson.*;
import io.mapsmessaging.rest.responses.DestinationResponse;

public class ServerDestinationStatusRequest extends RestApiConnection {

  public ServerDestinationStatusRequest(String url, String username, String password) {
    super(url, "/api/v1/server/destinations", username, password);
  }

  @Override
  public Object parse(JsonElement jsonElement) {
    return gson.fromJson(jsonElement, DestinationResponse.class);
  }
}
