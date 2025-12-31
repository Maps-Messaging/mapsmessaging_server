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

package io.mapsmessaging.api.transformers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.network.protocol.transformation.SchemaToJsonTransformation;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static io.mapsmessaging.api.transformers.TestAvroSchemas.SCHEMA_JSON;
import static io.mapsmessaging.api.transformers.TestAvroSchemas.avroSchemaConfig;
import static org.junit.jupiter.api.Assertions.*;

class AvroSchemaToJsonTest extends BaseTestConfig {

  private SchemaManager schemaManager;

  private UUID schemaId;

  @BeforeEach
  void setUp() {
    schemaManager = SchemaManager.getInstance();
  }

  @AfterEach
  void tearDown() {
    schemaManager.removeSchema(schemaId.toString());
  }

  @Test
  void avro_binaryPayload_isConvertedToJson() {
    schemaId = UUID.randomUUID();

    SchemaConfig schemaConfig = avroSchemaConfig(schemaId, SCHEMA_JSON);
    schemaManager.addSchema("/avro", schemaConfig);

    byte[] payload = TestAvroSchemas.encodeAvroPayload();

    SchemaToJsonTransformation transform = new SchemaToJsonTransformation();


    MessageBuilder builder = TestMessages.builderWithSchema(schemaId.toString(), payload);
    Message message = builder.build();
    message = transform.outgoing(message, "/avro");
    byte[] outPayload = message.getOpaqueData();
    JsonObject json = JsonParser.parseString(new String(outPayload, StandardCharsets.UTF_8)).getAsJsonObject();
    JsonObject payloadJson = json.getAsJsonObject("payload");

    assertEquals("abc", payloadJson.get("id").getAsString());
    assertEquals(123, payloadJson.get("count").getAsInt());
    assertTrue(payloadJson.get("active").getAsBoolean());
  }

  @Test
  void avro_missingSchema_dropsMessage() {
    schemaId = UUID.randomUUID();

    byte[] payload = TestAvroSchemas.encodeAvroPayload();

    SchemaToJsonTransformation transform = new SchemaToJsonTransformation();

    MessageBuilder builder = TestMessages.builderWithSchema(schemaId.toString(), payload);
    Message message = builder.build();

    Message out = transform.outgoing(message, "/avro"); // not registered

    assertNull(out);
  }

  @Test
  void avro_truncatedPayload_dropsMessage() {
    schemaId = UUID.randomUUID();

    SchemaConfig schemaConfig = avroSchemaConfig(schemaId, SCHEMA_JSON);
    schemaManager.addSchema("/avro", schemaConfig);

    byte[] payload = TestAvroSchemas.encodeAvroPayload();
    byte[] truncated = new byte[Math.max(0, payload.length - 1)];
    System.arraycopy(payload, 0, truncated, 0, truncated.length);

    SchemaToJsonTransformation transform = new SchemaToJsonTransformation();

    MessageBuilder builder = TestMessages.builderWithSchema(schemaId.toString(), truncated);
    Message message = builder.build();

    Message out = transform.outgoing(message, "/avro");

    assertNull(out);
  }
}
