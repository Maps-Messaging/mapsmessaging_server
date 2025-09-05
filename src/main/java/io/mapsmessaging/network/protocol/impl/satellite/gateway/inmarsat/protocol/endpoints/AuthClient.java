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

// src/main/java/io/mapsmessaging/network/protocol/impl/orbcomm/inmarsat/AuthClient.java
package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.endpoints;

import com.google.gson.Gson;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.AccessToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class AuthClient extends BaseInmarsatClient implements AuthReset {
  private static final int DEFAULT_SKEW_SECONDS = 30;
  private final AtomicReference<Cached> cache = new AtomicReference<>();

  public AuthClient(URI baseUrl, HttpClient http, Gson gson) {
    super(baseUrl, http, gson, null);
  }

  public void loadAccessToken(String clientId, String clientSecret) {
    URI reqUri = base.resolve("oauth/token");
    HttpRequest req = HttpRequest.newBuilder(reqUri)
        .header("ClientId", Objects.requireNonNull(clientId))
        .header("ClientSecret", Objects.requireNonNull(clientSecret))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();
    AccessToken token = send(req, AccessToken.class);
    cache.set(new Cached(token, Instant.now()));
  }

  public String getValidBearer(String clientId, String clientSecret) {
    Cached c = cache.get();
    Instant now = Instant.now();
    if (c == null || now.isAfter(c.token.expiresAt(c.obtainedAt, DEFAULT_SKEW_SECONDS))) {
      synchronized (this) {
        c = cache.get();
        if (c == null || now.isAfter(c.token.expiresAt(c.obtainedAt, DEFAULT_SKEW_SECONDS))) {
          loadAccessToken(clientId, clientSecret);
          c = cache.get();
        }
      }
    }
    return c.token.getToken();
  }

  public void resetAuth() {
    cache.set(null);
  }

  private record Cached(AccessToken token, Instant obtainedAt) {
  }
}
