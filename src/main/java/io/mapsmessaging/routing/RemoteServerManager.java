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

package io.mapsmessaging.routing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.routing.manager.SchemaMonitor;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RemoteServerManager implements Runnable {

  private final String baseUrl;
  private final SchemaMonitor schemaManager;
  private final HttpClient httpClient;
  private ScheduledFuture<?> scheduledFuture;

  public RemoteServerManager(String url, boolean schemaEnabled) {
    this.baseUrl = url;
    this.schemaManager = schemaEnabled ? new SchemaMonitor(url) : null;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    resume();
  }

  public synchronized void stop() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
      scheduledFuture = null;
    }
  }

  public void pause() {
    stop();
  }

  public synchronized void resume() {
    if (scheduledFuture == null) {
      scheduledFuture = SimpleTaskScheduler.getInstance().schedule(this, 60, TimeUnit.SECONDS);
    }
  }

  @Override
  public void run() {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/v1/updates"))
        .timeout(Duration.ofSeconds(20))
        .GET()
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (schemaManager != null && json.has("schemaUpdate")) {
          schemaManager.scanForUpdates(json.get("schemaUpdate").getAsLong());
        }
      } else {
        throw new RuntimeException("Request failed: " + response.statusCode());
      }
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Request failed", e);
    } finally {
      scheduledFuture = null;
      resume();
    }
  }
}
