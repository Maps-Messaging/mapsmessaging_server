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

package io.mapsmessaging.routing.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SchemaMonitor implements Runnable {

  private final String remoteUrl;
  private final HttpClient httpClient;
  private long lastUpdateCount;

  public SchemaMonitor(String remoteUrl) {
    this.remoteUrl = remoteUrl;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    lastUpdateCount = 0;
  }

  public void scanForUpdates(long lastUpdateCount) {
    if (this.lastUpdateCount != lastUpdateCount) {
      this.lastUpdateCount = lastUpdateCount;
      run();
    }
  }

  @Override
  public void run() {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(remoteUrl + "/api/v1/server/schema/"))
        .timeout(Duration.ofSeconds(20))
        .GET()
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray jsonArray = json.getAsJsonArray("data");

        if (jsonArray != null && !jsonArray.isEmpty()) {
          for (JsonElement element : jsonArray) {
            SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(element.toString());
            if (SchemaManager.getInstance().getSchema(config.getUniqueId()) == null) {
              SchemaManager.getInstance().addSchema(" ", config);
            }
          }
        }
      } else {
        throw new RuntimeException("Request failed: " + response.statusCode());
      }
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Request failed", e);
    }
  }
}
