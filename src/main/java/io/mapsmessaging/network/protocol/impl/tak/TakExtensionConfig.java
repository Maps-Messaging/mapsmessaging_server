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

package io.mapsmessaging.network.protocol.impl.tak;

import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.protocol.impl.tak.framing.TakStreamFramer;

import java.util.Locale;
import java.util.Map;

public class TakExtensionConfig {

  public static final String PAYLOAD_COT_XML = "cot_xml";
  public static final String PAYLOAD_TAK_PROTO_V1 = "tak_proto_v1";
  private static final int DEFAULT_MAX_PAYLOAD = 1024 * 1024;
  private static final int DEFAULT_RECONNECT_MS = 2000;
  private static final int DEFAULT_READ_BUFFER = 8192;
  private static final String DEFAULT_MULTICAST_GROUP = "239.2.3.1";
  private static final int DEFAULT_MULTICAST_PORT = 6969;
  private static final int DEFAULT_MULTICAST_TTL = 1;

  private final String payload;
  private final TakStreamFramer.Mode framingMode;
  private final int maxPayloadBytes;
  private final int reconnectDelayMs;
  private final int readBufferBytes;
  private final boolean multicastEnabled;
  private final boolean multicastIngressEnabled;
  private final boolean multicastEgressEnabled;
  private final String multicastGroup;
  private final int multicastPort;
  private final String multicastInterface;
  private final int multicastTtl;
  private final int multicastReadBufferBytes;

  private TakExtensionConfig(String payload, TakStreamFramer.Mode framingMode, int maxPayloadBytes, int reconnectDelayMs, int readBufferBytes,
                             boolean multicastEnabled, boolean multicastIngressEnabled, boolean multicastEgressEnabled,
                             String multicastGroup, int multicastPort, String multicastInterface, int multicastTtl, int multicastReadBufferBytes) {
    this.payload = payload;
    this.framingMode = framingMode;
    this.maxPayloadBytes = maxPayloadBytes;
    this.reconnectDelayMs = reconnectDelayMs;
    this.readBufferBytes = readBufferBytes;
    this.multicastEnabled = multicastEnabled;
    this.multicastIngressEnabled = multicastIngressEnabled;
    this.multicastEgressEnabled = multicastEgressEnabled;
    this.multicastGroup = multicastGroup;
    this.multicastPort = multicastPort;
    this.multicastInterface = multicastInterface;
    this.multicastTtl = multicastTtl;
    this.multicastReadBufferBytes = multicastReadBufferBytes;
  }

  public static TakExtensionConfig from(ExtensionConfigDTO extensionConfig) {
    Map<String, Object> config = extensionConfig != null ? extensionConfig.getConfig() : null;
    String payload = asString(config, "payload", PAYLOAD_COT_XML).toLowerCase(Locale.ROOT);
    String framing = asString(config, "framing", "xml_stream").toLowerCase(Locale.ROOT);
    TakStreamFramer.Mode mode = "proto_stream".equals(framing) ? TakStreamFramer.Mode.PROTO_STREAM : TakStreamFramer.Mode.XML_STREAM;
    int maxPayload = asInt(config, "max_payload_bytes", DEFAULT_MAX_PAYLOAD);
    int reconnectMs = asInt(config, "reconnect_delay_ms", DEFAULT_RECONNECT_MS);
    int readBuffer = asInt(config, "read_buffer_bytes", DEFAULT_READ_BUFFER);
    boolean multicastEnabled = asBoolean(config, "multicast_enabled", false);
    boolean multicastIngressEnabled = asBoolean(config, "multicast_ingress_enabled", multicastEnabled);
    boolean multicastEgressEnabled = asBoolean(config, "multicast_egress_enabled", multicastEnabled);
    String multicastGroup = asString(config, "multicast_group", DEFAULT_MULTICAST_GROUP).trim();
    int multicastPort = asInt(config, "multicast_port", DEFAULT_MULTICAST_PORT);
    String multicastInterface = asString(config, "multicast_interface", "").trim();
    int multicastTtl = asInt(config, "multicast_ttl", DEFAULT_MULTICAST_TTL);
    int multicastReadBuffer = asInt(config, "multicast_read_buffer_bytes", DEFAULT_READ_BUFFER);
    return new TakExtensionConfig(payload, mode, Math.max(1024, maxPayload), Math.max(100, reconnectMs), Math.max(512, readBuffer),
        multicastEnabled, multicastIngressEnabled, multicastEgressEnabled,
        multicastGroup.isEmpty() ? DEFAULT_MULTICAST_GROUP : multicastGroup,
        Math.max(1, multicastPort),
        multicastInterface,
        Math.max(1, Math.min(255, multicastTtl)),
        Math.max(512, multicastReadBuffer));
  }

  public String getPayload() {
    return payload;
  }

  public TakStreamFramer.Mode getFramingMode() {
    return framingMode;
  }

  public int getMaxPayloadBytes() {
    return maxPayloadBytes;
  }

  public int getReconnectDelayMs() {
    return reconnectDelayMs;
  }

  public int getReadBufferBytes() {
    return readBufferBytes;
  }

  public boolean isMulticastEnabled() {
    return multicastEnabled;
  }

  public boolean isMulticastIngressEnabled() {
    return multicastIngressEnabled;
  }

  public boolean isMulticastEgressEnabled() {
    return multicastEgressEnabled;
  }

  public String getMulticastGroup() {
    return multicastGroup;
  }

  public int getMulticastPort() {
    return multicastPort;
  }

  public String getMulticastInterface() {
    return multicastInterface;
  }

  public int getMulticastTtl() {
    return multicastTtl;
  }

  public int getMulticastReadBufferBytes() {
    return multicastReadBufferBytes;
  }

  private static String asString(Map<String, Object> config, String key, String def) {
    if (config == null) {
      return def;
    }
    Object val = config.get(key);
    return val == null ? def : val.toString();
  }

  private static int asInt(Map<String, Object> config, String key, int def) {
    if (config == null) {
      return def;
    }
    Object val = config.get(key);
    if (val == null) {
      return def;
    }
    if (val instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(val.toString());
    } catch (NumberFormatException ignored) {
      return def;
    }
  }

  private static boolean asBoolean(Map<String, Object> config, String key, boolean def) {
    if (config == null) {
      return def;
    }
    Object val = config.get(key);
    if (val == null) {
      return def;
    }
    if (val instanceof Boolean bool) {
      return bool;
    }
    return Boolean.parseBoolean(val.toString());
  }
}
