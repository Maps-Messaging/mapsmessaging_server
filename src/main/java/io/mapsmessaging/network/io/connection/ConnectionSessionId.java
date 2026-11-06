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

package io.mapsmessaging.network.io.connection;

import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Stable namespace identities for persistent MQTT network connections. */
public final class ConnectionSessionId {
  private ConnectionSessionId() { }

  /**
   * Hashes the base ID and sorted namespace list without modifying the list.
   * Length prefixes distinguish input boundaries; duplicate entries remain significant.
   * The 23 hexadecimal characters fit legacy MQTT client-ID constraints.
   */
  public static String derive(String sessionId, List<String> namespaces) {
    Objects.requireNonNull(sessionId, "sessionId");
    List<String> sorted = new ArrayList<>(List.copyOf(namespaces));
    sorted.sort(String::compareTo);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      update(digest, "maps-network-session-v1");
      update(digest, sessionId);
      for (String namespace : sorted) {
        update(digest, namespace);
      }
      return HexFormat.of().formatHex(digest.digest()).substring(0, 23);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  /** Captures each direction and both namespace paths, including schema forwarding. */
  public static List<String> namespaces(List<LinkConfigDTO> links) {
    if (links == null) {
      return List.of();
    }
    List<String> namespaces = new ArrayList<>(links.size());
    for (LinkConfigDTO link : links) {
      String direction = Objects.requireNonNull(link.getDirection(), "direction").toLowerCase(Locale.ROOT);
      String local = Objects.requireNonNull(link.getLocalNamespace(), "localNamespace");
      String remote = Objects.requireNonNull(link.getRemoteNamespace(), "remoteNamespace");
      namespaces.add(part(direction) + part(local) + part(remote) + (link.isIncludeSchema() ? "1" : "0"));
    }
    return List.copyOf(namespaces);
  }

  private static String part(String value) {
    return value.length() + ":" + value;
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
    digest.update(bytes);
  }
}
