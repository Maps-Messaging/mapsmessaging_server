/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.license;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LicenseServerClient {

  private static final String LICENSE_SERVER_URL = "https://license.mapsmessaging.io/api/v1/license";

  private final Logger logger;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final URI serverUrl;

  public LicenseServerClient(Logger logger) {
    this(logger, URI.create(LICENSE_SERVER_URL));
  }

  public LicenseServerClient(Logger logger, URI serverUrl) {
    this.logger = logger;
    this.serverUrl = serverUrl;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();
    this.objectMapper = new ObjectMapper();
  }

  public List<LicenseServerResponse> fetchLicenses(String clientName, String clientSecret, String uniqueId, UUID serverUUID) {
    try {
      String jsonBody = String.format(
          "{\"clientName\":\"%s\",\"clientSecret\":\"%s\",\"uniqueServerId\":\"%s\",\"serverUUID\":\"%s\"}",
          clientName, clientSecret, uniqueId, serverUUID.toString()
      );

      HttpRequest request = HttpRequest.newBuilder()
          .uri(serverUrl)
          .timeout(Duration.ofSeconds(5))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
          .build();

      logger.log(ServerLogMessages.LICENSE_CONTACTING_SERVER, clientName);

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      int responseCode = response.statusCode();

      if (responseCode != 200) {
        logger.log(ServerLogMessages.LICENSE_ERROR_CONTACTING_SERVER, responseCode);
        return List.of();
      }

      return parseResponse(response.body());
    } catch (Exception e) {
      logger.log(ServerLogMessages.LICENSE_FAILED_CONTACTING_SERVER, e);
      return List.of();
    }
  }

  private List<LicenseServerResponse> parseResponse(String responseBody) {
    try {
      List<Map<String, String>> mapList =
          objectMapper.readValue(responseBody, new TypeReference<List<Map<String, String>>>() {});

      if (mapList == null || mapList.isEmpty()) {
        return List.of();
      }

      List<LicenseServerResponse> results = new ArrayList<>();
      Base64.Decoder decoder = Base64.getDecoder();

      for (Map<String, String> licenseData : mapList) {
        String type = licenseData.get("type");
        String encodedLicense = licenseData.get("license");

        if (type == null || encodedLicense == null) {
          continue;
        }

        byte[] licenseContent = decoder.decode(encodedLicense);
        results.add(new LicenseServerResponse(type, licenseContent));
      }

      return results;
    } catch (Exception e) {
      return List.of();
    }
  }
}