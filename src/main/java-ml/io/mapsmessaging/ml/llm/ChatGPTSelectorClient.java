package io.mapsmessaging.ml.llm;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ChatGPTSelectorClient {

  private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

  private final String model;
  private final String apiKey;

  public ChatGPTSelectorClient(String model, String apiKey) {
    this.model = model;
    this.apiKey = apiKey;
  }

  public String generateSelector(String schemaJson, String contextHint) throws IOException, InterruptedException {
    String fullPrompt = buildPrompt(schemaJson, contextHint);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ENDPOINT))
        .timeout(Duration.ofSeconds(30))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(fullPrompt)))
        .build();

    HttpClient client = HttpClient.newHttpClient();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    return extractContent(response.body());
  }

  private String buildPrompt(String schema, String contextHint) {
    return String.format("""
            You are a system that generates JMS selector expressions for anomaly detection.\n
            Schema events are sensor readings arriving at the server when changed or in a cycle of 1/10th of seconds to minutes.\n
            Only use numeric fields from the schema that represent sensor values (not timestamps or status flags).\n
            Return only one selector expression like:\n
            isolation_forest(is_anomaly, model_name.arff, field1, field2, ...) = 1\n
            The schema may contain nested objects (e.g., SensorDataSchema), but assume the incoming event is flattened and fields like CO₂, temperature are top-level. Do not include prefixes like SensorDataSchema. in the selector.\n
            Context: %s\n
            Schema:\n%s
        """, contextHint, schema);
  }

  private String buildRequestBody(String prompt) {
    return """
        {
          "model": "%s",
          "messages": [
            { "role": "system", "content": "You are an expert in sensor data anomaly detection." },
            { "role": "user", "content": %s }
          ],
          "temperature": 0.2
        }
        """.formatted(model, escapeJson(prompt));
  }

  private String escapeJson(String text) {
    return "\"" + text.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n") + "\"";
  }

  private String extractContent(String responseBody) {
    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
    return json.getAsJsonArray("choices")
        .get(0).getAsJsonObject()
        .getAsJsonObject("message")
        .get("content").getAsString()
        .trim();
  }
}
