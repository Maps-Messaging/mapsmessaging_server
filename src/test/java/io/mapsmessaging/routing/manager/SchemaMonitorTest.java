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

package io.mapsmessaging.routing.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaMonitorTest {

  private HttpServer httpServer;
  private AtomicInteger requestCount;
  private AtomicReference<String> requestMethod;
  private AtomicReference<String> requestPath;
  private AtomicReference<String> responseBody;
  private AtomicInteger responseStatus;
  private String baseUrl;

  @BeforeEach
  void setUp() throws IOException {
    requestCount = new AtomicInteger();
    requestMethod = new AtomicReference<>();
    requestPath = new AtomicReference<>();
    responseBody = new AtomicReference<>("{\"data\":[]}");
    responseStatus = new AtomicInteger(200);

    httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext("/", this::handleRequest);
    httpServer.start();
    baseUrl = "http://127.0.0.1:" + httpServer.getAddress().getPort();
  }

  @AfterEach
  void tearDown() {
    httpServer.stop(0);
  }

  @Test
  void scanForUpdates_onlyRequestsWhenUpdateCountChanges() {
    SchemaMonitor monitor = new SchemaMonitor(baseUrl);

    monitor.scanForUpdates(0);
    monitor.scanForUpdates(1);
    monitor.scanForUpdates(1);
    monitor.scanForUpdates(2);

    assertEquals(2, requestCount.get());
  }

  @Test
  void run_requestsSchemaEndpointAndAcceptsEmptyData() {
    SchemaMonitor monitor = new SchemaMonitor(baseUrl);

    monitor.run();

    assertEquals("GET", requestMethod.get());
    assertEquals("/api/v1/server/schema/", requestPath.get());
    assertEquals(1, requestCount.get());
  }

  @Test
  void run_acceptsSuccessfulResponseWithoutDataProperty() {
    responseBody.set("{}");
    SchemaMonitor monitor = new SchemaMonitor(baseUrl);

    monitor.run();

    assertEquals(1, requestCount.get());
  }

  @Test
  void run_rejectsNonSuccessfulResponse() {
    responseStatus.set(503);
    SchemaMonitor monitor = new SchemaMonitor(baseUrl);

    RuntimeException exception = assertThrows(RuntimeException.class, monitor::run);

    assertEquals("Request failed: 503", exception.getMessage());
    assertEquals(1, requestCount.get());
  }

  private void handleRequest(HttpExchange exchange) throws IOException {
    requestCount.incrementAndGet();
    requestMethod.set(exchange.getRequestMethod());
    requestPath.set(exchange.getRequestURI().getPath());

    byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(responseStatus.get(), body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }
}
