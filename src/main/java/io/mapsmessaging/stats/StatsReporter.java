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
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.LicenseConfig;
import io.mapsmessaging.config.MessageDaemonConfig;
import io.mapsmessaging.dto.helpers.ServerStatisticsHelper;
import io.mapsmessaging.dto.helpers.StatusMessageHelper;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StatsReporter {

  private static final String REPORTING_URL = "https://stats.mapsmessaging.io/api/v1/report";

  private int minuteInterval;
  private ScheduledFuture<?> task;

  public StatsReporter() {
    minuteInterval = 15;
    queueTask();
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
    sendStats(buildBody());
  }

  private Map<String, String> buildBody(){
    LicenseConfig config = LicenseConfig.getInstance();
    Map<String, String> map = new LinkedHashMap<>();
    Gson gson = new Gson();
    Map<String, String> stats = new LinkedHashMap<>();
    stats.put("stats", gson.toJson(ServerStatisticsHelper.create()));
    stats.put("info", gson.toJson(StatusMessageHelper.fromMessageDaemon(MessageDaemon.getInstance())));
    map.put("serverUUID", MessageDaemon.getInstance().getUuid().toString());
    map.put("serverName", MessageDaemon.getInstance().getId());
    map.put("name", config.getClientName());
    map.put("secret", config.getClientSecret());
    map.put("serverstats", gson.toJson(stats));
    return map;
  }

  private void sendStats(Map<String, String> map) {
    try {
      if(!ConfigurationManager.getInstance().getConfiguration(MessageDaemonConfig.class).isSendAnonymousStatusUpdates()){
        return;
      }
      HttpURLConnection connection = (HttpURLConnection) new URL(REPORTING_URL).openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");  // Fix: Use JSON instead of form-data

      // Convert map to JSON
      Gson gson = new Gson();
      String jsonPayload = gson.toJson(map);

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

}
