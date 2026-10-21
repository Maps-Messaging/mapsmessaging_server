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

package io.mapsmessaging.network.protocol.sasl;

import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.AuthenticationMechanism;
import io.mapsmessaging.security.identity.IdentityLookup;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import lombok.Getter;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SaslAuthenticationMechanism implements AuthenticationMechanism, AutoCloseable {

  static final String SCRAM_SHA_256 = "SCRAM-SHA-256";
  static final String PLAIN = "PLAIN";
  private static final int MAX_CHALLENGES = 4;

  private final SaslServer saslServer;
  private int challengeCount;
  private String username;
  private boolean closed;

  @Getter
  private final String mechanism;

  public SaslAuthenticationMechanism(String mechanism, String serverName, String protocol, Map<String, String> props, EndPointServerConfigDTO properties) throws IOException {
    validateMechanism(mechanism, properties);
    SaslConfigDTO saslConfig = properties.getSaslConfig();
    IdentityLookup identityLookup;
    ServerCallbackHandler serverCallbackHandler;
    if (saslConfig.getIdentityProvider().equalsIgnoreCase("system")) {
      identityLookup = IdentityLookupFactory.getInstance().getSiteWide("system");
    } else {
      identityLookup = IdentityLookupFactory.getInstance().get(saslConfig.getIdentityProvider(), saslConfig.getSaslEntries());
    }
    if(identityLookup == null){
      throw new SaslException("Unable to locate identity look up mechanism for " + saslConfig.getSaslEntries());
    }
    serverCallbackHandler = new ServerCallbackHandler(serverName, identityLookup);
    Map<String, String> effectiveProperties = props == null ? new HashMap<>() : new HashMap<>(props);
    if (!PLAIN.equals(mechanism)) {
      effectiveProperties.put(Sasl.POLICY_NOPLAINTEXT, Boolean.TRUE.toString());
    }
    saslServer = Sasl.createSaslServer(mechanism, protocol, serverName, effectiveProperties, serverCallbackHandler);
    if (saslServer == null) {
      throw new IOException("Unsupported Sasl Mechanism : " + mechanism);
    }
    this.mechanism = mechanism;
    challengeCount = 0;
    closed = false;
  }

  @Override
  public byte[] challenge(byte[] challenge) throws IOException {
    if (closed) {
      throw new SaslException("SASL authentication is closed");
    }
    if (++challengeCount > MAX_CHALLENGES) {
      throw new SaslException("SASL authentication exceeded the permitted number of exchanges");
    }
    byte[] response = saslServer.evaluateResponse(challenge == null ? new byte[0] : challenge);
    if (saslServer.isComplete()) {
      username = saslServer.getAuthorizationID();
    }
    return response;
  }

  @Override
  public boolean complete() {
    if (saslServer == null) {
      return true;
    }
    return saslServer.isComplete();
  }

  public String getName(){
    return mechanism;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public void close() throws SaslException {
    if (!closed) {
      saslServer.dispose();
      closed = true;
    }
  }

  static boolean isSupportedMechanism(String mechanism) {
    return SCRAM_SHA_256.equals(mechanism) || PLAIN.equals(mechanism);
  }

  static boolean isProtectedTransport(String url) {
    if (url == null) {
      return false;
    }
    String scheme;
    try {
      scheme = URI.create(url).getScheme();
    } catch (IllegalArgumentException e) {
      return false;
    }
    if (scheme == null) {
      return false;
    }
    return switch (scheme.toLowerCase(Locale.ROOT)) {
      case "ssl", "dtls", "wss" -> true;
      default -> false;
    };
  }

  private void validateMechanism(String mechanism, EndPointServerConfigDTO properties) throws SaslException {
    if (!isSupportedMechanism(mechanism)) {
      throw new SaslException("Unsupported SASL mechanism: " + mechanism);
    }
    if (PLAIN.equals(mechanism) && !isProtectedTransport(properties.getUrl())) {
      throw new SaslException("PLAIN SASL requires SSL, DTLS, or WSS transport");
    }
  }
}
