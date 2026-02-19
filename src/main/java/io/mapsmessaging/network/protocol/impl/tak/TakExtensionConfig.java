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

  private final String payload;
  private final TakStreamFramer.Mode framingMode;
  private final int maxPayloadBytes;
  private final int reconnectDelayMs;
  private final int readBufferBytes;

  private TakExtensionConfig(String payload, TakStreamFramer.Mode framingMode, int maxPayloadBytes, int reconnectDelayMs, int readBufferBytes) {
    this.payload = payload;
    this.framingMode = framingMode;
    this.maxPayloadBytes = maxPayloadBytes;
    this.reconnectDelayMs = reconnectDelayMs;
    this.readBufferBytes = readBufferBytes;
  }

  public static TakExtensionConfig from(ExtensionConfigDTO extensionConfig) {
    Map<String, Object> config = extensionConfig != null ? extensionConfig.getConfig() : null;
    String payload = asString(config, "payload", PAYLOAD_COT_XML).toLowerCase(Locale.ROOT);
    String framing = asString(config, "framing", "xml_stream").toLowerCase(Locale.ROOT);
    TakStreamFramer.Mode mode = "proto_stream".equals(framing) ? TakStreamFramer.Mode.PROTO_STREAM : TakStreamFramer.Mode.XML_STREAM;
    int maxPayload = asInt(config, "max_payload_bytes", DEFAULT_MAX_PAYLOAD);
    int reconnectMs = asInt(config, "reconnect_delay_ms", DEFAULT_RECONNECT_MS);
    int readBuffer = asInt(config, "read_buffer_bytes", DEFAULT_READ_BUFFER);
    return new TakExtensionConfig(payload, mode, Math.max(1024, maxPayload), Math.max(100, reconnectMs), Math.max(512, readBuffer));
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
}
