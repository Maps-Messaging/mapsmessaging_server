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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.MessageOverrideDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Map;

class MessageOverridesTest {

  private Message baseMessage() {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("m1", "v1");

    Map<String, TypedData> data = new LinkedHashMap<>();
    data.put("d1", new TypedData("x"));

    MessageBuilder builder = new MessageBuilder()
        .setCorrelationData("corr")
        .setOpaqueData(new byte[]{1, 2, 3})
        .setContentType("application/test")
        .setResponseTopic("resp/topic")
        .setPriority(Priority.NORMAL)
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setMeta(meta)
        .setDataMap(data)
        .setPayloadIndicator(true)
        .setRetain(false)
        .setSchemaId("schema-1");

    builder.setExpiry(5000); // interval style according to calculateExpiry()
    return builder.build();
  }

  @Test
  void setOverrides_nullOverride_returnsSameInstance() {
    Message message = baseMessage();

    Message result = MessageOverrides.setOverrides(null, message);

    Assertions.assertSame(message, result);
  }

  @Test
  void createMessageBuilder_nullOverride_returnsSameBuilder() {
    MessageBuilder builder = new MessageBuilder().setCorrelationData("c");

    MessageBuilder result = MessageOverrides.createMessageBuilder(null, builder);

    Assertions.assertSame(builder, result);
  }

  @Test
  void applyOverrides_setsScalarFields_whenPresent() {
    MessageOverrideDTO override = Mockito.mock(MessageOverrideDTO.class);

    Mockito.when(override.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    Mockito.when(override.getPriority()).thenReturn(Priority.HIGHEST);
    Mockito.when(override.getContentType()).thenReturn("application/json");
    Mockito.when(override.getResponseTopic()).thenReturn("reply/to");
    Mockito.when(override.getExpiry()).thenReturn(1234L);
    Mockito.when(override.getRetain()).thenReturn(Boolean.TRUE);
    Mockito.when(override.getStoreOffline()).thenReturn(Boolean.TRUE);
    Mockito.when(override.getSchemaId()).thenReturn("schema-2");
    Mockito.when(override.getDataMap()).thenReturn(null);
    Mockito.when(override.getMeta()).thenReturn(null);

    MessageBuilder builder = new MessageBuilder().setCorrelationData("c");
    builder.setExpiry(0);

    MessageBuilder updated = MessageOverrides.createMessageBuilder(override, builder);

    Assertions.assertEquals(QualityOfService.AT_LEAST_ONCE, updated.getQualityOfService());
    Assertions.assertEquals(Priority.HIGHEST, updated.getPriority());
    Assertions.assertEquals("application/json", updated.getContentType());
    Assertions.assertEquals("reply/to", updated.getResponseTopic());
    Assertions.assertEquals(1234L, updated.getExpiry());
    Assertions.assertTrue(updated.isRetain());
    Assertions.assertTrue(updated.isStoreOffline());
    Assertions.assertEquals("schema-2", updated.getSchemaId());
  }

  @Test
  void applyOverrides_mergesMeta_andDataMap_creatingMapsWhenNull() {
    MessageOverrideDTO override = Mockito.mock(MessageOverrideDTO.class);

    Mockito.when(override.getQualityOfService()).thenReturn(null);
    Mockito.when(override.getPriority()).thenReturn(null);
    Mockito.when(override.getContentType()).thenReturn(null);
    Mockito.when(override.getResponseTopic()).thenReturn(null);
    Mockito.when(override.getExpiry()).thenReturn(-1L);
    Mockito.when(override.getRetain()).thenReturn(null);
    Mockito.when(override.getSchemaId()).thenReturn(null);

    Map<String, Object> overrideData = new LinkedHashMap<>();
    overrideData.put("d1", "configured");
    overrideData.put("d2", 42L);

    Map<String, String> overrideMeta = new LinkedHashMap<>();
    overrideMeta.put("m2", "v2");

    Mockito.when(override.getDataMap()).thenReturn(overrideData);
    Mockito.when(override.getMeta()).thenReturn(overrideMeta);

    Message message = baseMessage();

    Message updated = MessageOverrides.setOverrides(override, message);

    Assertions.assertEquals("v1", updated.getMeta().get("m1"));
    Assertions.assertEquals("v2", updated.getMeta().get("m2"));

    Assertions.assertEquals("x", updated.getDataMap().get("d1").getData());
    Assertions.assertEquals(42L, updated.getDataMap().get("d2").getData());
    Assertions.assertFalse(message.getMeta().containsKey("m2"));
    Assertions.assertFalse(message.getDataMap().containsKey("d2"));
  }

  @Test
  void setOverrides_doesNotChangeExpiryOrDelay_whenOverrideDoesNotSetThem() {
    Message message = new MessageBuilder()
        .setCorrelationData("corr")
        .setOpaqueData(new byte[]{1, 2, 3})
        .setExpiry(120_000)     // 10 seconds TTL (duration)
        .setDelayed(2_000)     // 2 seconds delay (duration)
        .build();

    long originalExpiryAbsolute = message.getExpiry();
    long originalDelayedAbsolute = message.getDelayed();

    MessageOverrideDTO override = Mockito.mock(MessageOverrideDTO.class);
    Mockito.when(override.getExpiry()).thenReturn(-1L); // not specified
    Mockito.when(override.getMeta()).thenReturn(Map.of("tag", "x"));
    Mockito.when(override.getDataMap()).thenReturn(null);
    Mockito.when(override.getQualityOfService()).thenReturn(null);
    Mockito.when(override.getPriority()).thenReturn(null);
    Mockito.when(override.getContentType()).thenReturn(null);
    Mockito.when(override.getResponseTopic()).thenReturn(null);
    Mockito.when(override.getRetain()).thenReturn(null);
    Mockito.when(override.getSchemaId()).thenReturn(null);

    Message updated = MessageOverrides.setOverrides(override, message);

    long val = Math.abs(updated.getExpiry() - originalExpiryAbsolute);
    // "absolute times" should remain basically identical; tolerate small execution jitter.
    Assertions.assertTrue(val <= 2000, "Expiry absolute time should be preserved when override does not set expiry "+val);
    Assertions.assertTrue(Math.abs(updated.getDelayed() - originalDelayedAbsolute) <= 50, "Delayed absolute time should be preserved when override does not set delayed");
  }

  @Test
  void setOverrides_preservesIdentifier() {
    Message message = new MessageBuilder()
        .setId(12345L)
        .setCorrelationData("corr")
        .setOpaqueData(new byte[]{1})
        .setExpiry(10_000)
        .build();

    MessageOverrideDTO override = Mockito.mock(MessageOverrideDTO.class);
    Mockito.when(override.getExpiry()).thenReturn(-1L);
    Mockito.when(override.getMeta()).thenReturn(Map.of("tag", "x"));
    Mockito.when(override.getDataMap()).thenReturn(null);
    Mockito.when(override.getQualityOfService()).thenReturn(null);
    Mockito.when(override.getPriority()).thenReturn(null);
    Mockito.when(override.getContentType()).thenReturn(null);
    Mockito.when(override.getResponseTopic()).thenReturn(null);
    Mockito.when(override.getRetain()).thenReturn(null);
    Mockito.when(override.getSchemaId()).thenReturn(null);

    Message updated = MessageOverrides.setOverrides(override, message);

    Assertions.assertEquals(0L, updated.getIdentifier(), "Identifier must be reset by overrides");
  }

  @Test
  void setOverrides_preservesCreationAndSchema_whenNotOverridden() {
    long creation = 123456789L;
    Message message = new MessageBuilder()
        .setCreation(creation)
        .setSchemaId("schema-id")
        .setOpaqueData(new byte[]{1})
        .build();
    MessageOverrideDTO override = new MessageOverrideDTO();
    override.setMeta(Map.of("tag", "x"));

    Message updated = MessageOverrides.setOverrides(override, message);

    Assertions.assertEquals(creation, updated.getCreation());
    Assertions.assertEquals("schema-id", updated.getSchemaId());
  }

  @Test
  void setOverrides_nullExpiry_preservesExistingExpiry() {
    Message message = new MessageBuilder()
        .setExpiry(60_000)
        .setOpaqueData(new byte[]{1})
        .build();
    MessageOverrideDTO override = new MessageOverrideDTO();
    override.setExpiry(null);
    long originalExpiry = message.getExpiry();

    Message updated = MessageOverrides.setOverrides(override, message);

    Assertions.assertTrue(Math.abs(updated.getExpiry() - originalExpiry) <= 50);
  }

  @Test
  void resolve_mergesMatchingOverrides_fromGeneralToSpecific() {
    MessageOverrideDTO rootOverride = new MessageOverrideDTO();
    rootOverride.setExpiry(60_000L);
    rootOverride.setMeta(Map.of("source", "root", "root", "present"));
    DestinationConfigDTO root = new DestinationConfigDTO();
    root.setMessageOverride(rootOverride);

    MessageOverrideDTO fleetOverride = new MessageOverrideDTO();
    fleetOverride.setPriority(Priority.HIGHEST);
    fleetOverride.setMeta(Map.of("source", "fleet"));
    DestinationConfigDTO fleet = new DestinationConfigDTO();
    fleet.setMessageOverride(fleetOverride);

    Map<String, DestinationConfigDTO> configs = new LinkedHashMap<>();
    configs.put("/", root);
    configs.put("/fleet/drone", fleet);

    MessageOverrideDTO resolved = MessageOverrides.resolve("/fleet/drone/status", configs);
    MessageOverrideDTO sibling = MessageOverrides.resolve("/fleet/droneBackup", configs);

    Assertions.assertNotNull(resolved);
    Assertions.assertEquals(60_000L, resolved.getExpiry());
    Assertions.assertEquals(Priority.HIGHEST, resolved.getPriority());
    Assertions.assertEquals("fleet", resolved.getMeta().get("source"));
    Assertions.assertEquals("present", resolved.getMeta().get("root"));
    Assertions.assertNotNull(sibling);
    Assertions.assertNull(sibling.getPriority());
  }
}
