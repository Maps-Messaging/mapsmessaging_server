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

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

class LicenseServerClientTest {

  private MockWebServer mockWebServer;

  @BeforeEach
  void setup() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mockWebServer != null) {
      mockWebServer.shutdown();
    }
  }

  @Test
  void fetchLicenses_success_returnsDecodedLicenses() throws Exception {
    byte[] licenseBytes = "test-license-bytes".getBytes(StandardCharsets.UTF_8);
    String base64License = Base64.getEncoder().encodeToString(licenseBytes);

    String responseJson =
        "[" +
            "{\"type\":\"community\",\"license\":\"" + base64License + "\"}" +
            "]";

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(responseJson)
    );

    URI serverUri = mockWebServer.url("/api/v1/license").uri();

    Logger logger = LoggerFactory.getLogger(LicenseServerClientTest.class);
    LicenseServerClient licenseServerClient = new LicenseServerClient(logger, serverUri);

    String clientName = "build-client";
    String clientSecret = "build-secret";
    String uniqueId = "build";
    UUID serverUuid = UUID.fromString("11111111-2222-3333-4444-555555555555");

    List<LicenseServerResponse> responses =
        licenseServerClient.fetchLicenses(clientName, clientSecret, uniqueId, serverUuid);

    Assertions.assertNotNull(responses);
    Assertions.assertEquals(1, responses.size());

    LicenseServerResponse first = responses.get(0);
    Assertions.assertEquals("community", first.getType());
    Assertions.assertArrayEquals(licenseBytes, first.getLicenseContent());

    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    Assertions.assertEquals("POST", recordedRequest.getMethod());
    Assertions.assertEquals("/api/v1/license", recordedRequest.getPath());
    Assertions.assertEquals("application/json", recordedRequest.getHeader("Content-Type"));

    String postedBody = recordedRequest.getBody().readUtf8();
    Assertions.assertTrue(postedBody.contains("\"clientName\":\"" + clientName + "\""));
    Assertions.assertTrue(postedBody.contains("\"clientSecret\":\"" + clientSecret + "\""));
    Assertions.assertTrue(postedBody.contains("\"uniqueServerId\":\"" + uniqueId + "\""));
    Assertions.assertTrue(postedBody.contains("\"serverUUID\":\"" + serverUuid + "\""));
  }

  @Test
  void fetchLicenses_non200_returnsEmptyList() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"nope\"}")
    );

    URI serverUri = mockWebServer.url("/api/v1/license").uri();

    Logger logger = LoggerFactory.getLogger(LicenseServerClientTest.class);
    LicenseServerClient licenseServerClient = new LicenseServerClient(logger, serverUri);

    List<LicenseServerResponse> responses =
        licenseServerClient.fetchLicenses("client", "secret", "build", UUID.randomUUID());

    Assertions.assertNotNull(responses);
    Assertions.assertTrue(responses.isEmpty());
  }

  @Test
  void fetchLicenses_invalidJson_returnsEmptyList() throws Exception {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("this is not json")
    );

    URI serverUri = mockWebServer.url("/api/v1/license").uri();

    Logger logger = LoggerFactory.getLogger(LicenseServerClientTest.class);
    LicenseServerClient licenseServerClient = new LicenseServerClient(logger, serverUri);

    List<LicenseServerResponse> responses =
        licenseServerClient.fetchLicenses("client", "secret", "build", UUID.randomUUID());

    Assertions.assertNotNull(responses);
    Assertions.assertTrue(responses.isEmpty());
  }

  @Test
  void fetchLicenses_invalidBase64_skipsEntryOrReturnsEmpty() throws Exception {
    String responseJson =
        "[" +
            "{\"type\":\"community\",\"license\":\"NOT_BASE64!!!!\"}" +
            "]";

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(responseJson)
    );

    URI serverUri = mockWebServer.url("/api/v1/license").uri();

    Logger logger = LoggerFactory.getLogger(LicenseServerClientTest.class);
    LicenseServerClient licenseServerClient = new LicenseServerClient(logger, serverUri);

    List<LicenseServerResponse> responses =
        licenseServerClient.fetchLicenses("client", "secret", "build", UUID.randomUUID());

    Assertions.assertNotNull(responses);
    Assertions.assertTrue(responses.isEmpty());
  }
}