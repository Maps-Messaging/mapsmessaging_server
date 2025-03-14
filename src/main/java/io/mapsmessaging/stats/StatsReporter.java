package io.mapsmessaging.stats;

import com.google.gson.Gson;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.LicenseConfig;
import io.mapsmessaging.dto.helpers.ServerStatisticsHelper;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class StatsReporter {

  private static final String REPORTING_URL ="https://stats.mapsmessaging.io/api/v1/report";

  private int minuteInterval;
  private ScheduledFuture<?> task;

  public StatsReporter() {
    minuteInterval = 1;
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
    map.put("serverId", MessageDaemon.getInstance().getId());
    map.put("name", config.getClientName());
    map.put("secret", config.getClientSecret());
    map.put("serverstats", gson.toJson(ServerStatisticsHelper.create()));
    return map;
  }

  private void sendStats(Map<String, String> map) {
    try {
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
