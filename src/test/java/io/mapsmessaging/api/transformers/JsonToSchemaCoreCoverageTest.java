/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToSchemaTransformationDTO;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonToSchemaCoreCoverageTest {

  @Test
  void identityMetadataDescribesTransformation() {
    JsonToSchema transformation = new JsonToSchema();

    assertEquals("JsonToSchema", transformation.getName());
    assertEquals("Converts JSON to Schema specified and configured", transformation.getDescription());
  }

  @Test
  void buildCreatesConfiguredTransformation() {
    JsonToSchema factory = new JsonToSchema();
    JsonToSchemaTransformationDTO dto = new JsonToSchemaTransformationDTO();

    InterServerTransformation built = factory.build(dto);

    assertInstanceOf(JsonToSchema.class, built);
  }

  @Test
  void transformPacksJsonUsingLoadedFormatter() throws Exception {
    JsonToSchema transformation = new JsonToSchema();
    SchemaConfig schema = mock(SchemaConfig.class);
    MessageFormatter formatter = mock(MessageFormatter.class);
    byte[] packed = new byte[]{9,8,7};
    when(formatter.parseFromJson(any())).thenReturn(packed);
    set(transformation, "schemaConfig", schema);
    set(transformation, "messageFormatter", formatter);

    var original = new MessageBuilder()
        .setOpaqueData("{\"value\":42}".getBytes(StandardCharsets.UTF_8))
        .build();
    ParsedMessage parsed = new ParsedMessage("/target", original);

    ParsedMessage result = transformation.transform("/source", parsed);

    assertSame(parsed, result);
    assertArrayEquals(packed, result.getMessage().getOpaqueData());
    verify(formatter).parseFromJson(any());
  }

  @Test
  void transformWithoutFormatterLeavesPayloadUnchanged() throws Exception {
    JsonToSchema transformation = new JsonToSchema();
    set(transformation, "schemaConfig", null);
    set(transformation, "messageFormatter", null);
    byte[] payload = "{\"value\":42}".getBytes(StandardCharsets.UTF_8);
    ParsedMessage parsed = new ParsedMessage(
        "/target",
        new MessageBuilder().setOpaqueData(payload).build());

    ParsedMessage result = transformation.transform("/source", parsed);

    assertArrayEquals(payload, result.getMessage().getOpaqueData());
  }

  @Test
  void invalidJsonIsCaughtAndOriginalPayloadIsPreserved() throws Exception {
    JsonToSchema transformation = new JsonToSchema();
    set(transformation, "schemaConfig", mock(SchemaConfig.class));
    set(transformation, "messageFormatter", mock(MessageFormatter.class));
    byte[] payload = "not-json".getBytes(StandardCharsets.UTF_8);
    ParsedMessage parsed = new ParsedMessage(
        "/target",
        new MessageBuilder().setOpaqueData(payload).build());

    assertDoesNotThrow(() -> transformation.transform("/source", parsed));
    assertArrayEquals(payload, parsed.getMessage().getOpaqueData());
  }

  @Test
  void formatterFailureIsCaughtAndOriginalPayloadIsPreserved() throws Exception {
    JsonToSchema transformation = new JsonToSchema();
    SchemaConfig schema = mock(SchemaConfig.class);
    MessageFormatter formatter = mock(MessageFormatter.class);
    when(formatter.parseFromJson(any())).thenThrow(new java.io.IOException("bad format"));
    set(transformation, "schemaConfig", schema);
    set(transformation, "messageFormatter", formatter);
    byte[] payload = "{\"value\":42}".getBytes(StandardCharsets.UTF_8);
    ParsedMessage parsed = new ParsedMessage(
        "/target",
        new MessageBuilder().setOpaqueData(payload).build());

    transformation.transform("/source", parsed);

    assertArrayEquals(payload, parsed.getMessage().getOpaqueData());
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = JsonToSchema.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
