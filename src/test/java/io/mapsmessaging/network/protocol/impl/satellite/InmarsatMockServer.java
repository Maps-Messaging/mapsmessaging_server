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

package io.mapsmessaging.network.protocol.impl.satellite;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class InmarsatMockServer {

  @FunctionalInterface
  interface IoHandler { void run() throws IOException; }

  private final HttpServer httpServer;
  private final int serverPort;

  private final Map<String, MobileTerminatedDetail> mobileTerminatedById = new ConcurrentHashMap<>();
  private final Set<String> validTokens = ConcurrentHashMap.newKeySet();

  private final Random random = new Random(42);
  private final String accessTokenValue = "test-access-token";

  // Logging controls
  private final boolean logHeaders;
  private final boolean logBodies;
  private static final int MAX_LOG_BODY = 4096;
  private final AtomicLong requestSeq = new AtomicLong(1);
  private final String base = "/v1";
  private final Queue<byte[]> incomingMessages;
  private final Queue<byte[]> outgoingMessages;
  private final String deviceId = "000000000SKYEE3D";


  public InmarsatMockServer( Queue<byte[]> incomingMessages,  Queue<byte[]> outgoingMessages, int serverPort) throws IOException {
    this(incomingMessages, outgoingMessages, serverPort, getEnvBool("INMARSAT_MOCK_LOG_HEADERS", true), getEnvBool("INMARSAT_MOCK_LOG_BODIES", true));
  }

  public InmarsatMockServer(Queue<byte[]> incomingMessages,  Queue<byte[]> outgoingMessages, int serverPort, boolean logHeaders, boolean logBodies) throws IOException {
    this.serverPort = serverPort;
    this.logHeaders = logHeaders;
    this.logBodies = logBodies;
    this.httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
    this.httpServer.setExecutor(Executors.newCachedThreadPool());
    this.incomingMessages = incomingMessages;
    this.outgoingMessages = outgoingMessages;

    // Contexts
    this.httpServer.createContext( "/oauth/token", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      logRequestStart(reqId, exchange, null);
      try {
        handleToken(exchange, reqId, t0);
      } catch (IOException ioe) {
        // Best-effort error response logging
        sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false);
      }
    });



    // Mailboxes list (no auth)
    this.httpServer.createContext(base+"/mailbox", exchange -> {
      String reqId = nextRequestId(); long t0 = System.nanoTime();
      try { handleMailbox(exchange, reqId, t0); }
      catch (IOException ioe) { sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false); }
    });

    this.httpServer.createContext(base + "/info/errors", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      logRequestStart(reqId, exchange, null);
      try {
        handleErrors(exchange, reqId, t0);
      } catch (IOException ioe) {
        sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false);
      }
    });

    // ---- Context wiring (constructor) ----
    httpServer.createContext(base+"/device", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      try {
        withAuth(exchange, true, reqId, t0, () -> handleDevice(exchange, reqId, t0));
      } catch (IOException ioe) {
        sendJson(exchange, 500, errorJson("E500", "Internal error"), reqId, t0, false);
      }
    });


    this.httpServer.createContext(base + "/messages/mobileTerminated/mode", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      try {
        withAuth(exchange, true, reqId, t0, () -> handleCommands(exchange, "mode", reqId, t0));
      } catch (IOException ioe) {
        sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false);
      }
    });

    this.httpServer.createContext(base + "/messages/mobileTerminated/mute", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      try {
        withAuth(exchange, true, reqId, t0, () -> handleCommands(exchange, "mute", reqId, t0));
      } catch (IOException ioe) {
        sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false);
      }
    });

    this.httpServer.createContext(base + "/messages/mobileTerminated/reset", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      try {
        withAuth(exchange, true, reqId, t0, () -> handleCommands(exchange, "reset", reqId, t0));
      } catch (IOException ioe) {
        sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false);
      }
    });

    this.httpServer.createContext(base + "/messages/mobileTerminated", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      try {
        withAuth(exchange, true, reqId, t0, () -> {
          String method = exchange.getRequestMethod();
          if ("POST".equalsIgnoreCase(method)) {
            handleSubmitMobileTerminated(exchange, reqId, t0);
          } else if ("GET".equalsIgnoreCase(method)) {
            handleMobileTerminatedDetails(exchange, reqId, t0);
          } else {
            sendJson(exchange, 405, errorJson("E405", "Method Not Allowed"), reqId, t0, false);
          }
        });
      } catch (IOException ioe) {
        sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false);
      }
    });

    this.httpServer.createContext(base + "/messages/mobileTerminated/status", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      try {
        withAuth(exchange, true, reqId, t0, () -> handleMobileTerminatedStatus(exchange, reqId, t0));
      } catch (IOException ioe) {
        sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false);
      }
    });

    this.httpServer.createContext(base + "/messages/mobileOriginated", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      try {
        withAuth(exchange, true, reqId, t0, () -> handleMobileOriginated(exchange, reqId, t0));
      } catch (IOException ioe) {
        sendJson(exchange, 500, errorJson("E500","Internal error"), reqId, t0, false);
      }
    });
    httpServer.createContext("/", exchange -> {
      String reqId = nextRequestId();
      long t0 = System.nanoTime();
      String method = exchange.getRequestMethod();
      String uri = exchange.getRequestURI().toString();

      // Log the incoming request
      logRequest(reqId, exchange, readBody(exchange), false);

      // Return 404
      String body = errorJson("E404", "No handler for " + method + " " + uri);
      sendJson(exchange, 404, body, reqId, t0, logBodies);
    });
  }

  public void start() { httpServer.start(); System.out.println("Inmarsat mock server on http://localhost:" + serverPort); }
  public void stop() { httpServer.stop(0); }

  // ===== Models =====
  private static final class MobileTerminatedDetail {
    String id;
    String mobileId;
    String submittedUtc;
    String status;
    String lastUpdateUtc;
    String payload;
    String payloadEncoding;
    int sin;
  }

  // ===== Handlers =====
  private void handleToken(HttpExchange exchange, String reqId, long t0) throws IOException {
    applyCommonHeaders(exchange);
    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
      sendJson(exchange, 405, errorJson("E405", "Method Not Allowed"), reqId, t0, false);
      return;
    }
    Headers headers = exchange.getRequestHeaders();
    String clientId = headers.getFirst("ClientId");
    String clientSecret = headers.getFirst("ClientSecret");
    logRequestStart(reqId, exchange, null); // no body for token
    if (isBlank(clientId) || isBlank(clientSecret)) {
      sendJson(exchange, 401, errorJson("E401", "Missing ClientId or ClientSecret"), reqId, t0, false);
      return;
    }
    maybeSleep(queryParam(exchange, "latencyMs"));
    validTokens.add(accessTokenValue);
    String issuedAt = Instant.now().toString();
    String response = "{\"access_token\":\"" + accessTokenValue + "\"," +
        "\"token_type\":\"Bearer\"," +
        "\"expires_in\":3600," +
        "\"issued_at\":\"" + issuedAt + "\"}";
    sendJson(exchange, 200, response, reqId, t0, logBodies);
  }

  private void handleErrors(HttpExchange exchange, String reqId, long t0) throws IOException {
    applyCommonHeaders(exchange);
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      sendJson(exchange, 405, errorJson("E405", "Method Not Allowed"), reqId, t0, false);
      return;
    }
    logRequestStart(reqId, exchange, null);
    maybeSleep(queryParam(exchange, "latencyMs"));
    String response = "[" +
        "{\"code\":\"E001\",\"message\":\"Invalid Mobile ID\"}," +
        "{\"code\":\"E002\",\"message\":\"Mailbox not found\"}," +
        "{\"code\":\"E003\",\"message\":\"Rate limit exceeded\"}," +
        "{\"code\":\"E004\",\"message\":\"Invalid payload encoding\"}" +
        "]";
    sendJson(exchange, 200, response, reqId, t0, logBodies);
  }

  private void handleMailbox(HttpExchange exchange, String requestId, long startNanos) throws IOException {
    applyCommonHeaders(exchange);
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      sendJson(exchange, 405, errorJson("E405", "Method Not Allowed"), requestId, startNanos, false);
      return;
    }

    boolean trace = "true".equalsIgnoreCase(queryParam(exchange, "trace"));
    logRequest(requestId, exchange, null, trace);

    String mailboxId = exchange.getRequestHeaders().getFirst("X-Mailbox");
    if (mailboxId == null || mailboxId.trim().isEmpty()) {
      sendJson(exchange, 400, errorJson("E400", "X-Mailbox header required"), requestId, startNanos, false);
      return;
    }

    Mailbox mailbox = new Mailbox();
    mailbox.mailboxId = mailboxId;
    mailbox.alias = "Alias for " + mailboxId;
    mailbox.enabled = true;
    mailbox.provisioningTime = Instant.now().toString();
    mailbox.codecFileName = "codec-v1.2.bin";
    mailbox.codecFileUploadTime = Instant.now().toString();
    mailbox.numberOfActiveDevices = 1 + random.nextInt(5);
    mailbox.numberOfActiveBroadcasts = random.nextInt(3);

    StringBuilder json = new StringBuilder(256);
    json.append("{")
        .append("\"mailboxId\":\"").append(escapeJson(nullToEmpty(mailbox.mailboxId))).append("\",")
        .append("\"alias\":\"").append(escapeJson(nullToEmpty(mailbox.alias))).append("\",")
        .append("\"enabled\":").append(mailbox.enabled == null ? "null" : mailbox.enabled.toString()).append(",")
        .append("\"provisioningTime\":\"").append(escapeJson(nullToEmpty(mailbox.provisioningTime))).append("\",")
        .append("\"codecFileName\":\"").append(escapeJson(nullToEmpty(mailbox.codecFileName))).append("\",")
        .append("\"codecFileUploadTime\":\"").append(escapeJson(nullToEmpty(mailbox.codecFileUploadTime))).append("\",")
        .append("\"numberOfActiveDevices\":").append(mailbox.numberOfActiveDevices == null ? "null" : mailbox.numberOfActiveDevices.toString()).append(",")
        .append("\"numberOfActiveBroadcasts\":").append(mailbox.numberOfActiveBroadcasts == null ? "null" : mailbox.numberOfActiveBroadcasts.toString())
        .append("}");

    sendJson(exchange, 200, json.toString(), requestId, startNanos, logBodies || trace);
  }

  // Minimal model for the mock (package-private inside the server class is fine)
  private static final class Mailbox {
    String mailboxId;
    String alias;
    Boolean enabled;
    String provisioningTime;
    String codecFileName;
    String codecFileUploadTime;
    Integer numberOfActiveDevices;
    Integer numberOfActiveBroadcasts;
  }

  // tiny helper
  private static String nullToEmpty(String value) { return value == null ? "" : value; }

  private void handleCommands(HttpExchange exchange, String operation, String reqId, long t0) throws IOException {
    applyCommonHeaders(exchange);
    String method = exchange.getRequestMethod();
    if (!"POST".equalsIgnoreCase(method) && !"GET".equalsIgnoreCase(method)) {
      sendJson(exchange, 405, errorJson("E405", "Method Not Allowed"), reqId, t0, false);
      return;
    }
    if (!requireMailbox(exchange, reqId, t0)) return;
    if (checkForcedError(exchange, reqId, t0)) return;

    maybeSleep(queryParam(exchange, "latencyMs"));
    boolean trace = "true".equalsIgnoreCase(queryParam(exchange, "trace"));

    String requestBody = readBody(exchange);
    logRequest(reqId, exchange, requestBody, trace);

    List<String> mobileIdList = extractAllStringFieldValues(requestBody, "mobileId");
    boolean randomReject = "true".equalsIgnoreCase(queryParam(exchange, "randomReject"));
    List<String> accepted = new ArrayList<>();
    List<String> rejected = new ArrayList<>();

    for (String mobileId : mobileIdList) {
      if (!isValidMobileId(mobileId)) {
        rejected.add(generateId("mt-err"));
        continue;
      }
      boolean shouldReject = randomReject && random.nextDouble() < 0.2;
      if (shouldReject) {
        rejected.add(generateId("mt-rej"));
      } else {
        String id = generateId("mt");
        accepted.add(id);

        MobileTerminatedDetail detail = new MobileTerminatedDetail();
        detail.id = id;
        detail.mobileId = mobileId;
        detail.submittedUtc = Instant.now().toString();
        detail.status = "SENT";
        detail.lastUpdateUtc = detail.submittedUtc;
        detail.payload = "";
        detail.payloadEncoding = "HEX";
        detail.sin = 0;
        mobileTerminatedById.put(id, detail);
      }
    }

    String submissionId = generateId("sub");
    StringJoiner acceptedJson = new StringJoiner(",", "[", "]");
    for (String s : accepted) acceptedJson.add("\"" + s + "\"");
    StringJoiner rejectedJson = new StringJoiner(",", "[", "]");
    for (String s : rejected) rejectedJson.add("\"" + s + "\"");

    String response = "{\"submissionId\":\"" + submissionId + "\"," +
        "\"accepted\":" + acceptedJson + "," +
        "\"rejected\":" + rejectedJson + "}";
    sendJson(exchange, 200, response, reqId, t0, logBodies || trace);
  }

  private void handleSubmitMobileTerminated(HttpExchange exchange, String requestId, long startNanos) throws IOException {
    applyCommonHeaders(exchange);
    if (!requireMailbox(exchange, requestId, startNanos)) return;
    if (checkForcedError(exchange, requestId, startNanos)) return;

    maybeSleep(queryParam(exchange, "latencyMs"));
    boolean trace = "true".equalsIgnoreCase(queryParam(exchange, "trace"));

    String requestBody = readBody(exchange);
    logRequest(requestId, exchange, requestBody, trace);

    List<String> destinationIdList = extractAllStringFieldValues(requestBody, "destinationId");
    List<String> payloadRawList   = extractAllStringFieldValues(requestBody, "payloadRaw");

    List<String> accepted = new ArrayList<>();
    List<String> rejected = new ArrayList<>();

    int count = Math.max(destinationIdList.size(), payloadRawList.size());
    for (int i = 0; i < count; i++) {
      String destinationId = i < destinationIdList.size() ? destinationIdList.get(i) : null;
      String payloadRaw    = i < payloadRawList.size()   ? payloadRawList.get(i)   : null;

      boolean hasDestination = destinationId != null && !destinationId.isBlank();
      if(payloadRaw != null) {
        if (payloadRaw.startsWith("\"")) {
          payloadRaw = payloadRaw.substring(1, payloadRaw.length() - 1);
        }
        int idx = payloadRaw.indexOf('\"');
        if (idx != -1) {
          payloadRaw = payloadRaw.substring(0, idx);
        }
        payloadRaw = payloadRaw.replace("\\u003d", "=");
      }

      boolean hasPayload     = payloadRaw != null && !payloadRaw.isBlank();

      // Validate Base64 if present
      boolean base64Ok = false;
      if (hasPayload) {
        try {
          java.util.Base64.getDecoder().decode(payloadRaw);
          base64Ok = true;
        } catch (IllegalArgumentException ignore) {
          ignore.printStackTrace();
          base64Ok = false;
        }
      }

      if (!hasDestination || !hasPayload || !base64Ok) {
        rejected.add(generateId("mt-rej"));
        continue;
      }

      String messageId = generateId("mt");
      accepted.add(messageId);
      outgoingMessages.add(java.util.Base64.getDecoder().decode(payloadRaw));
    }

    String submissionId = generateId("sub");
    StringJoiner acceptedJson = new StringJoiner(",", "[", "]");
    for (String s : accepted) acceptedJson.add("\"" + s + "\"");
    StringJoiner rejectedJson = new StringJoiner(",", "[", "]");
    for (String s : rejected) rejectedJson.add("\"" + s + "\"");

    String response = "{\"submissionId\":\"" + submissionId + "\"," +
        "\"accepted\":" + acceptedJson + "," +
        "\"rejected\":" + rejectedJson + "}";
    sendJson(exchange, 200, response, requestId, startNanos, logBodies || trace);
  }

  // ---- Handler ----
  private void handleDevice(HttpExchange exchange, String requestId, long startNanos) throws IOException {
    applyCommonHeaders(exchange);
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      sendJson(exchange, 405, errorJson("E405", "Method Not Allowed"), requestId, startNanos, false);
      return;
    }
    if (!requireMailbox(exchange, requestId, startNanos)) return;

    boolean trace = "true".equalsIgnoreCase(queryParam(exchange, "trace"));
    logRequest(requestId, exchange, null, trace);

    String mailboxId = exchange.getRequestHeaders().getFirst("X-Mailbox");
    String mailboxAlias = "Alias for " + mailboxId;

    // Generate 2–4 synthetic devices deterministically per call
    int count = 2 + (Math.abs(mailboxId.hashCode()) % 3);

    StringBuilder sb = new StringBuilder(512);
    sb.append("[");
    for (int i = 0; i < count; i++) {
      if (i > 0) sb.append(",");


      String now = Instant.now().toString();
      String lastReg = Instant.now().minusSeconds(600L + i * 120L).toString();
      String lastUpd = Instant.now().minusSeconds(300L + i * 60L).toString();
      int wake = 600 + i * 60;                  // 10–12 min
      int opMode = i % 4;                       // 0..3
      int network = i % 2;                      // 0=IDP,1=OGx
      Integer registered = (i % 3 == 0) ? null : 1; // occasionally null

      sb.append("{")
          .append("\"mailboxId\":\"").append(escapeJson(mailboxId)).append("\",")
          .append("\"deviceId\":\"").append(escapeJson(deviceId)).append("\",")
          .append("\"provisioningTime\":\"").append(now).append("\",")
          .append("\"mailboxAlias\":\"").append(escapeJson(mailboxAlias)).append("\",")

          // RemoteDeviceInfo serialized names:
          .append("\"lastModemRegistrationTime\":\"").append(lastReg).append("\",")
          .append("\"lastUpdatedTime\":\"").append(lastUpd).append("\",")
          .append("\"wakeUpInterval\":").append(wake).append(",")
          .append("\"operationMode\":").append(opMode).append(",")
          .append("\"network\":").append(network).append(",")
          .append("\"IsRegistered\":").append(registered == null ? "null" : registered.toString())
          .append("}");
    }
    sb.append("]");

    sendJson(exchange, 200, sb.toString(), requestId, startNanos, logBodies || trace);
  }


  private void handleMobileTerminatedStatus(HttpExchange exchange, String reqId, long t0) throws IOException {
    applyCommonHeaders(exchange);
    if (!requireMailbox(exchange, reqId, t0)) return;
    if (checkForcedError(exchange, reqId, t0)) return;

    maybeSleep(queryParam(exchange, "latencyMs"));
    boolean trace = "true".equalsIgnoreCase(queryParam(exchange, "trace"));
    logRequest(reqId, exchange, null, trace);

    boolean paging = "true".equalsIgnoreCase(queryParam(exchange, "paging"));
    String pageToken = queryParam(exchange, "pageToken");

    List<MobileTerminatedDetail> all = new ArrayList<>(mobileTerminatedById.values());
    all.sort(Comparator.comparing(d -> d.id));

    int pageSize = 2;
    int startIndex = 0;
    if (paging && pageToken != null) {
      try { startIndex = Integer.parseInt(pageToken); } catch (NumberFormatException ignore) { }
    }

    int endIndex = paging ? Math.min(startIndex + pageSize, all.size()) : all.size();
    List<MobileTerminatedDetail> page = all.subList(startIndex, endIndex);

    StringBuilder json = new StringBuilder();
    json.append("{\"items\":[");
    boolean first = true;
    for (MobileTerminatedDetail d : page) {
      if (!first) json.append(",");
      first = false;
      json.append("{\"id\":\"").append(d.id).append("\",")
          .append("\"status\":\"").append(d.status).append("\",")
          .append("\"statusUtc\":\"").append(d.lastUpdateUtc).append("\"}");
    }
    json.append("],");
    if (paging && endIndex < all.size()) {
      json.append("\"nextPageToken\":\"").append(endIndex).append("\"");
    } else {
      json.append("\"nextPageToken\":null");
    }
    json.append("}");
    sendJson(exchange, 200, json.toString(), reqId, t0, logBodies || trace);
  }

  private void handleMobileTerminatedDetails(HttpExchange exchange, String reqId, long t0) throws IOException {
    applyCommonHeaders(exchange);
    if (!requireMailbox(exchange, reqId, t0)) return;
    if (checkForcedError(exchange, reqId, t0)) return;

    maybeSleep(queryParam(exchange, "latencyMs"));
    boolean trace = "true".equalsIgnoreCase(queryParam(exchange, "trace"));
    logRequest(reqId, exchange, null, trace);

    Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
    String idList = query.get("idList");
    if (isBlank(idList)) {
      sendJson(exchange, 400, errorJson("E400", "idList is required"), reqId, t0, logBodies || trace);
      return;
    }

    String[] ids = idList.split(",");
    StringBuilder json = new StringBuilder();
    json.append("[");
    boolean first = true;
    for (String id : ids) {
      MobileTerminatedDetail d = mobileTerminatedById.get(id.trim());
      if (d == null) continue;
      if (!first) json.append(",");
      first = false;
      json.append("{")
          .append("\"id\":\"").append(d.id).append("\",")
          .append("\"mobileId\":\"").append(d.mobileId).append("\",")
          .append("\"submittedUtc\":\"").append(d.submittedUtc).append("\",")
          .append("\"status\":\"").append(d.status).append("\",")
          .append("\"lastUpdateUtc\":\"").append(d.lastUpdateUtc).append("\",")
          .append("\"payload\":\"").append(escapeJson(d.payload)).append("\",")
          .append("\"payloadEncoding\":\"").append(d.payloadEncoding).append("\",")
          .append("\"sin\":").append(d.sin)
          .append("}");
    }
    json.append("]");
    sendJson(exchange, 200, json.toString(), reqId, t0, logBodies || trace);
  }

  private void handleMobileOriginated(HttpExchange exchange, String reqId, long t0) throws IOException {
    applyCommonHeaders(exchange);
    if (!requireMailbox(exchange, reqId, t0)) return;
    if (checkForcedError(exchange, reqId, t0)) return;

    maybeSleep(queryParam(exchange, "latencyMs"));
    boolean trace = "true".equalsIgnoreCase(queryParam(exchange, "trace"));
    logRequest(reqId, exchange, null, trace);

    Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
    String mailboxId = exchange.getRequestHeaders().getFirst("X-Mailbox");
    boolean includeRaw = "true".equalsIgnoreCase(params.getOrDefault("includeRawPayload", "true"));

    StringBuilder json = new StringBuilder();
    json.append("{\"messages\":[");
    boolean first = true;
    for (byte[] payload; (payload = incomingMessages.poll()) != null; ) {
      if (!first) json.append(",");
      first = false;

      String b64 = java.util.Base64.getEncoder().encodeToString(payload);

      String messageUtc = java.time.Instant.now().minusSeconds(1).toString();
      String receiveUtc = java.time.Instant.now().toString();

      json.append("{")
          .append("\"id\":\"").append(escapeJson(generateId("mo"))).append("\",")
          .append("\"deviceId\":\"").append(escapeJson(deviceId)).append("\",")
          .append("\"messageUtc\":\"").append(messageUtc).append("\",")
          .append("\"receiveUtc\":\"").append(receiveUtc).append("\",")
          .append("\"sin\":0,");
      if (includeRaw) {
        json.append("\"payloadRaw\":\"").append(b64).append("\",");
      }
      json.append("\"payload\":null,")
          .append("\"transport\":\"INMARSAT\"")
          .append("}");
    }
    String nextStartTime = java.time.Instant.now().toString();
    json.append("],\"nextStartTime\":\"").append(nextStartTime).append("\"}");
    sendJson(exchange, 200, json.toString(), reqId, t0, logBodies || trace);
  }

  // ===== Auth / Common =====
  private void withAuth(HttpExchange exchange, boolean mailboxRequired, String reqId, long t0, IoHandler handler) throws IOException {
    applyCommonHeaders(exchange);
    String method = exchange.getRequestMethod();
    if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
      logRequestStart(reqId, exchange, null);
      sendJson(exchange, 405, errorJson("E405", "Method Not Allowed"), reqId, t0, false);
      return;
    }

    String bearer = extractBearer(exchange.getRequestHeaders().getFirst("Authorization"));
    if (isBlank(bearer) || !validTokens.contains(bearer)) {
      logRequestStart(reqId, exchange, null);
      sendJson(exchange, 401, errorJson("E401", "Unauthorized"), reqId, t0, false);
      return;
    }
    if (mailboxRequired && !requireMailbox(exchange, reqId, t0)) return;

    handler.run();
  }

  private boolean requireMailbox(HttpExchange exchange, String reqId, long t0) throws IOException {
    String mailbox = exchange.getRequestHeaders().getFirst("X-Mailbox");
    if (isBlank(mailbox)) {
      logRequestStart(reqId, exchange, null);
      sendJson(exchange, 400, errorJson("E400", "X-Mailbox header required"), reqId, t0, false);
      return false;
    }
    return true;
  }

  private boolean checkForcedError(HttpExchange exchange, String reqId, long t0) throws IOException {
    String forced = queryParam(exchange, "forceError");
    if (!isBlank(forced)) {
      logRequestStart(reqId, exchange, null);
      sendJson(exchange, 400, "{\"error\":{\"code\":\"" + forced + "\",\"message\":\"Forced error\"}}", reqId, t0, logBodies);
      return true;
    }
    return false;
  }

  private void applyCommonHeaders(HttpExchange exchange) {
    Headers headers = exchange.getResponseHeaders();
    headers.add("Content-Type", "application/json; charset=utf-8");
    headers.add("Cache-Control", "no-store");
    headers.add("Access-Control-Allow-Origin", "*");
    headers.add("Access-Control-Allow-Headers", "Authorization, X-Mailbox, ClientId, ClientSecret, Content-Type");
    headers.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  }

  // ===== Utilities =====
  private String extractBearer(String authorizationHeader) {
    if (authorizationHeader == null) return null;
    String prefix = "Bearer ";
    if (authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
      return authorizationHeader.substring(prefix.length()).trim();
    }
    return null;
  }

  private String readBody(HttpExchange exchange) throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      exchange.getRequestBody().transferTo(outputStream);
      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }

  private void sendJson(HttpExchange exchange, int statusCode, String json, String reqId, long t0, boolean logBody) throws IOException {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
    long durationMs = (System.nanoTime() - t0) / 1_000_000;
    logResponse(reqId, statusCode, durationMs, bytes.length, logBody ? snippet(json) : null);
  }

  private Map<String, String> parseQuery(String rawQuery) {
    Map<String, String> result = new LinkedHashMap<>();
    if (rawQuery == null || rawQuery.isEmpty()) return result;
    String[] parts = rawQuery.split("&");
    for (String part : parts) {
      int idx = part.indexOf('=');
      if (idx >= 0) {
        String key = urlDecode(part.substring(0, idx));
        String value = urlDecode(part.substring(idx + 1));
        result.put(key, value);
      } else {
        result.put(urlDecode(part), "");
      }
    }
    return result;
  }

  private String urlDecode(String value) {
    try { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    catch (Exception e) { return value; }
  }

  private String queryParam(HttpExchange exchange, String name) {
    URI uri = exchange.getRequestURI();
    Map<String, String> query = parseQuery(uri.getRawQuery());
    return query.get(name);
  }

  private void maybeSleep(String latencyMs) {
    if (latencyMs == null) return;
    try {
      long delay = Long.parseLong(latencyMs);
      if (delay > 0 && delay < 60_000) Thread.sleep(delay);
    } catch (Exception ignore) { }
  }

  private boolean isValidMobileId(String mobileId) {
    if (mobileId == null || mobileId.length() != 15) return false;
    for (int i = 0; i < mobileId.length(); i++) {
      char c = mobileId.charAt(i);
      if (c < '0' || c > '9') return false;
    }
    return true;
  }

  private boolean isValidPayloadEncoding(String encoding) {
    if (encoding == null) return true;
    String v = encoding.toUpperCase(Locale.ROOT);
    return v.equals("HEX") || v.equals("BASE64") || v.equals("TEXT");
  }

  private String generateId(String prefix) {
    return prefix + "-" + Math.abs(random.nextInt(1_000_000));
  }

  private String escapeJson(String text) {
    if (text == null) return "";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
      }
    }
    return sb.toString();
  }

  private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

  private List<String> extractAllStringFieldValues(String json, String fieldName) {
    if (isBlank(json) || isBlank(fieldName)) return List.of();
    List<String> values = new ArrayList<>();
    String needle = "\"" + fieldName + "\"";
    int index = 0;
    while (true) {
      index = json.indexOf(needle, index);
      if (index < 0) break;
      int colon = json.indexOf(':', index + needle.length());
      if (colon < 0) break;
      int quoteStart = json.indexOf('"', colon + 1);
      if (quoteStart < 0) break;
      int quoteEnd = json.indexOf('"', quoteStart + 1);
      if (quoteEnd < 0) break;
      values.add(json.substring(quoteStart + 1, quoteEnd));
      index = quoteEnd + 1;
    }
    return values;
  }

  private String firstStringFieldValue(String json, String fieldName) {
    List<String> all = extractAllStringFieldValues(json, fieldName);
    return all.isEmpty() ? null : all.get(0);
  }

  private Integer firstIntFieldValue(String json, String fieldName) {
    if (isBlank(json) || isBlank(fieldName)) return null;
    String needle = "\"" + fieldName + "\"";
    int index = json.indexOf(needle);
    if (index < 0) return null;
    int colon = json.indexOf(':', index + needle.length());
    if (colon < 0) return null;
    int start = colon + 1;
    while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
    int end = start;
    while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
    try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return null; }
  }

  private String errorJson(String code, String message) {
    return "{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + escapeJson(message) + "\"}}";
  }

  // ===== Logging =====
  private static boolean getEnvBool(String name, boolean def) {
    String v = System.getenv(name);
    if (v == null) return def;
    return v.equalsIgnoreCase("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes");
  }

  private String nextRequestId() { return String.format("req-%04d", requestSeq.getAndIncrement()); }

  private void logRequestStart(String reqId, HttpExchange ex, String bodyOrNull) {
    String method = ex.getRequestMethod();
    String uri = ex.getRequestURI().toString();
    String ip = ex.getRemoteAddress() != null ? ex.getRemoteAddress().getAddress().getHostAddress() : "?";
    String mailbox = ex.getRequestHeaders().getFirst("X-Mailbox");
    String auth = ex.getRequestHeaders().getFirst("Authorization");
    int bodyLen = bodyOrNull == null ? -1 : bodyOrNull.getBytes(StandardCharsets.UTF_8).length;

    System.err.printf("→ %s %s %s ip=%s mailbox=%s auth=%s body=%s%n",
        reqId, method, uri, ip, safe(mailbox), safe(auth), bodyLen < 0 ? "-" : (bodyLen + "B"));

    if (logHeaders) {
      Headers h = ex.getRequestHeaders();
      for (String k : h.keySet()) {
        for (String v : h.get(k)) {
          if ("ClientSecret".equalsIgnoreCase(k)) v = "***";
          System.err.printf("   %s: %s%n", k, v);
        }
      }
    }
  }

  private void logRequest(String reqId, HttpExchange ex, String body, boolean trace) {
    logRequestStart(reqId, ex, body);
    if (logBodies || trace) {
      System.err.printf("   body: %s%n", snippet(body));
    }
  }

  private void logResponse(String reqId, int status, long durationMs, int bodyLen, String bodySnippetOrNull) {
    System.err.printf("← %s %d in %dms body=%dB%n", reqId, status, durationMs, bodyLen);
    if (bodySnippetOrNull != null) {
      System.err.printf("   body: %s%n", bodySnippetOrNull);
    }
  }

  private String snippet(String body) {
    if (body == null) return "null";
    if (body.length() <= MAX_LOG_BODY) return body;
    return body.substring(0, MAX_LOG_BODY) + " …(truncated)";
  }

  private String safe(String s) { return s == null ? "-" : s; }

}

