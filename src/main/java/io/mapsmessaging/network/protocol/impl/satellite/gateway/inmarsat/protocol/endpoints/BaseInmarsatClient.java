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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.endpoints;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BaseInmarsatClient {
  private static final String USER_AGENT = "MapsMessaging-InmarsatClient/1.0";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final int MAX_RETRIES = 3;
  private static final long JITTER_MS = 150;
  protected final URI base;           // e.g. https://apis.inmarsat.com/v1/
  protected final HttpClient http;
  protected final Gson gson;
  protected final AuthReset authReset;

  protected BaseInmarsatClient(URI base, HttpClient http, Gson gson, AuthReset authReset) {
    this.base = Objects.requireNonNull(base);
    this.http = Objects.requireNonNull(http);
    this.gson = Objects.requireNonNull(gson);
    this.authReset = authReset;
  }

  public static String xMailbox(String mailboxId, String mailboxPassword) {
    return Base64.getEncoder().encodeToString((mailboxId + ":" + mailboxPassword).getBytes(StandardCharsets.UTF_8));
  }

  private static boolean shouldRetry(int status) {
    return status == 429 || status == 502 || status == 503 || status == 504;
  }

  private static void sleepBackoff(int attempt, HttpHeaders headers) {
    long retryAfterMs = parseRetryAfter(headers).orElse(0L);
    long base = (long) Math.pow(2, attempt) * 250L; // 250ms, 500ms, 1000ms...
    long jitter = ThreadLocalRandom.current().nextLong(JITTER_MS + 1);
    long sleep = Math.max(retryAfterMs, base + jitter);
    try {
      Thread.sleep(sleep);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  private static Optional<Long> parseRetryAfter(HttpHeaders h) {
    Optional<String> ra = h.firstValue("Retry-After");
    if (ra.isEmpty()) return Optional.empty();
    String v = ra.get().trim();
    try {
      // seconds form
      long sec = Long.parseLong(v);
      return Optional.of(Math.max(0, sec) * 1000L);
    } catch (NumberFormatException ignore) {
      return Optional.empty(); // RFC-date format ignored for simplicity
    }
  }

  // ---------- Query-string helper ----------
  protected static String qs(Map<String, String> q) {
    if (q == null || q.isEmpty()) return "";
    StringJoiner sj = new StringJoiner("&");
    q.forEach((k, v) -> {
      if (v != null && !v.isBlank())
        sj.add(URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8));
    });
    String out = sj.toString();
    return out.isEmpty() ? "" : "?" + out;
  }

  // ---------- Public parse wrappers ----------
  protected <T> T send(HttpRequest req, Class<T> cls) {
    String body = doSendWithRetry(req);
    return gson.fromJson(body, cls);
  }

  protected <T> T send(HttpRequest req, Type type) {
    String body = doSendWithRetry(req);
    return gson.fromJson(body, type);
  }

  protected void sendVoid(HttpRequest req) {
    doSendWithRetry(req);
  }

  // ---------- Core send with retry/backoff ----------
  private String doSendWithRetry(HttpRequest req) {
    int attempt = 0;
    while (true) {
      try {
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code / 100 == 2) {
          return resp.body();
        }
        if(resp.statusCode() == 401){
          if(authReset != null) {
            authReset.resetAuth();
          }
        }
        if (shouldRetry(code) && attempt < MAX_RETRIES) {
          sleepBackoff(attempt, resp.headers());
          attempt++;
          continue;
        }

        throw errorFor(resp);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("HTTP request interrupted", ie);
      } catch (IOException ioe) {
        if (attempt < MAX_RETRIES) {
          sleepBackoff(attempt, HttpHeaders.of(Map.of(), (k, v) -> true));
          attempt++;
          continue;
        }
        throw new RuntimeException("HTTP transport failure", ioe);
      }
    }
  }

  private RuntimeException errorFor(HttpResponse<String> resp) {
    String body = resp.body();
    String msg = "HTTP " + resp.statusCode();
    try {
      ApiError e = gson.fromJson(body, ApiError.class);
      if (e != null ){
        msg += " - "+e;
      } else if (body != null && !body.isBlank()) {
        msg += " — " + body;
      }
    } catch (Exception ignore) {
      if (body != null && !body.isBlank()) msg += " — " + body;
    }
    return new RuntimeException(msg);
  }

  // ---------- With X-Mailbox ----------
  protected <T> T get(String path, Map<String, String> query, String bearer, String xMailbox, Class<T> cls) {
    HttpRequest req = baseRequest(path + qs(query), bearer)
        .header("X-Mailbox", Objects.requireNonNull(xMailbox))
        .GET().build();
    return send(req, cls);
  }

  protected <T> T get(String path, Map<String, String> query, String bearer, String xMailbox, Type type) {
    HttpRequest req = baseRequest(path + qs(query), bearer)
        .header("X-Mailbox", Objects.requireNonNull(xMailbox))
        .GET().build();
    return send(req, type);
  }

  protected <T> T postJson(String path, Object body, String bearer, String xMailbox, Class<T> cls) {
    HttpRequest req = baseRequest(path, bearer)
        .header("X-Mailbox", Objects.requireNonNull(xMailbox))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
        .build();
    return send(req, cls);
  }

  protected <T> T getWithBodyJson(String path, Object body, String bearer, String xMailbox, Class<T> cls) {
    HttpRequest req = baseRequest(path, bearer)
        .header("X-Mailbox", Objects.requireNonNull(xMailbox))
        .header("Content-Type", "application/json")
        .method("GET", HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
        .build();
    return send(req, cls);
  }

  // ---------- No X-Mailbox (VAR endpoints) ----------
  protected <T> T getNoMailbox(String path, Map<String, String> query, String bearer, Type type) {
    HttpRequest req = baseRequest(path + qs(query), bearer).GET().build();
    return send(req, type);
  }

  // ---------- Common base for all requests ----------
  private HttpRequest.Builder baseRequest(String path, String bearerOrNull) {
    HttpRequest.Builder b = HttpRequest.newBuilder(base.resolve(path))
        .timeout(REQUEST_TIMEOUT)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json");
    if (bearerOrNull != null && !bearerOrNull.isBlank()) {
      b.header("Authorization", "Bearer " + bearerOrNull);
    }
    return b;
  }

}
