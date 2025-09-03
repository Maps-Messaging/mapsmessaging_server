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

package io.mapsmessaging.stats;

import com.google.gson.Gson;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.LicenseConfig;
import io.mapsmessaging.config.MessageDaemonConfig;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.stats.data.ServerStats;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StatsReporter {

  private static final String REPORTING_URL =  "https://stats.mapsmessaging.io/api/v1/report";

  private int minuteInterval;
  private ScheduledFuture<?> task;

  public StatsReporter() {
    minuteInterval = 15;
    report();
  }

  private void queueTask(){
    task = SimpleTaskScheduler.getInstance().schedule(this::report, minuteInterval, TimeUnit.MINUTES);
  }

  public void close(){
    if(task != null){
      task.cancel(true);
    }
  }

  public void report() {
    String serverUUID = MessageDaemon.getInstance().getUuid().toString();
    String serverName = MessageDaemon.getInstance().getId();
    long uptime = System.currentTimeMillis() - MessageDaemon.getInstance().getStartTime();
    FeatureManager featureManager = MessageDaemon.getInstance().getFeatureManager();
    String licenseId = featureManager.getLoadedLicenses();
    ServerStats status = ServerStatsPopulator.collect(serverUUID, serverName, licenseId, BuildInfo.getBuildVersion(), uptime);
    sendStats(status);
  }

  private String buildJsonPayload(Gson gson, ServerStats status) {

    LicenseConfig config = LicenseConfig.getInstance();
    ServerStatsRequest request = new ServerStatsRequest();
    request.setName(config.getClientName());
    request.setSecret(config.getClientSecret());
    request.setServerUUID(MessageDaemon.getInstance().getUuid().toString());
    request.setServerName(MessageDaemon.getInstance().getId());
    request.setServerstats(gson.toJson(status));
    request.setVersion(1);
    return gson.toJson(request);
  }

  private void sendStats(ServerStats status) {
    try {
      if (!ConfigurationManager.getInstance().
          getConfiguration(MessageDaemonConfig.class).
          isSendAnonymousStatusUpdates()) {
        return;
      }
      HttpURLConnection connection = (HttpURLConnection) new URL(REPORTING_URL).openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");  // Fix: Use JSON instead of form-data
      Gson gson = new Gson();

      // Convert ServerStats to JSON
      String jsonPayload = buildJsonPayload(gson, status);


      // Send JSON request
      try (OutputStream os = connection.getOutputStream()) {
        os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        os.flush();
      }

      // Read the response
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          String response = reader.lines().collect(Collectors.joining());
          Map<String, Object> result = gson.fromJson(response, Map.class);
          if(result.containsKey("updateInterval")){
            minuteInterval = ((Number) result.get("updateInterval")).intValue();
          }
        }
      }
    } catch (Exception e) {
    }
    queueTask();
  }

  @Data
  private static class ServerStatsRequest {
    private String name;
    private String secret;
    private String serverUUID;
    private String serverName;
    private String serverstats;
    private int version;
  }

}
