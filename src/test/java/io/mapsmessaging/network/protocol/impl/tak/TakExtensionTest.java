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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakExtensionTest {

  @Test
  void parsesExtensionConfigWithDefaultsAndOverrides() throws Exception {
    ExtensionConfigDTO dto = new ExtensionConfigDTO();
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("payload", "tak_proto_v1");
    config.put("framing", "proto_stream");
    config.put("max_payload_bytes", 2048);
    config.put("reconnect_delay_ms", 1500);
    config.put("reconnect_max_delay_ms", 6000);
    config.put("reconnect_backoff_multiplier", 1.5);
    config.put("reconnect_jitter_ms", 100);
    config.put("read_buffer_bytes", 4096);
    config.put("multicast_enabled", true);
    config.put("multicast_ingress_enabled", true);
    config.put("multicast_egress_enabled", false);
    config.put("multicast_group", "239.88.77.66");
    config.put("multicast_port", 7171);
    config.put("multicast_interface", "lo0");
    config.put("multicast_ttl", 3);
    config.put("multicast_read_buffer_bytes", 2048);
    config.put("protobuf_descriptor_base64", "AQID");
    config.put("protobuf_message_name", "TakMessage");
    setField(dto, "config", config);

    TakExtensionConfig parsed = TakExtensionConfig.from(dto);

    assertEquals("tak_proto_v1", parsed.getPayload());
    assertEquals(TakStreamFramer.Mode.PROTO_STREAM, parsed.getFramingMode());
    assertEquals(2048, parsed.getMaxPayloadBytes());
    assertEquals(1500, parsed.getReconnectDelayMs());
    assertEquals(6000, parsed.getReconnectMaxDelayMs());
    assertEquals(1.5d, parsed.getReconnectBackoffMultiplier());
    assertEquals(100, parsed.getReconnectJitterMs());
    assertEquals(4096, parsed.getReadBufferBytes());
    assertTrue(parsed.isMulticastEnabled());
    assertTrue(parsed.isMulticastIngressEnabled());
    assertFalse(parsed.isMulticastEgressEnabled());
    assertEquals("239.88.77.66", parsed.getMulticastGroup());
    assertEquals(7171, parsed.getMulticastPort());
    assertEquals("lo0", parsed.getMulticastInterface());
    assertEquals(3, parsed.getMulticastTtl());
    assertEquals(2048, parsed.getMulticastReadBufferBytes());
    assertEquals("AQID", parsed.getProtobufDescriptorBase64());
    assertEquals("TakMessage", parsed.getProtobufMessageName());
  }

  @Test
  void appliesSafeDefaultsWhenMissing() {
    TakExtensionConfig parsed = TakExtensionConfig.from(null);
    assertEquals("cot_xml", parsed.getPayload());
    assertEquals(TakStreamFramer.Mode.XML_STREAM, parsed.getFramingMode());
    assertEquals(1024 * 1024, parsed.getMaxPayloadBytes());
    assertEquals(2000, parsed.getReconnectDelayMs());
    assertEquals(30000, parsed.getReconnectMaxDelayMs());
    assertEquals(2.0d, parsed.getReconnectBackoffMultiplier());
    assertEquals(250, parsed.getReconnectJitterMs());
    assertFalse(parsed.isMulticastEnabled());
    assertEquals("239.2.3.1", parsed.getMulticastGroup());
    assertEquals(6969, parsed.getMulticastPort());
    assertEquals("", parsed.getProtobufDescriptorBase64());
    assertEquals("", parsed.getProtobufMessageName());
  }

  @Test
  void clampsReconnectHardeningValues() throws Exception {
    ExtensionConfigDTO dto = new ExtensionConfigDTO();
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("reconnect_delay_ms", 50);
    config.put("reconnect_max_delay_ms", 10);
    config.put("reconnect_backoff_multiplier", 0.1d);
    config.put("reconnect_jitter_ms", -1);
    setField(dto, "config", config);

    TakExtensionConfig parsed = TakExtensionConfig.from(dto);

    assertEquals(100, parsed.getReconnectDelayMs());
    assertEquals(100, parsed.getReconnectMaxDelayMs());
    assertEquals(1.0d, parsed.getReconnectBackoffMultiplier());
    assertEquals(0, parsed.getReconnectJitterMs());
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
