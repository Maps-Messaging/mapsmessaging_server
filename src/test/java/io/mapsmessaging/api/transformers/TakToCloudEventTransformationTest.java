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

package io.mapsmessaging.api.transformers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.network.protocol.impl.tak.codec.CotXmlCodec;
import io.mapsmessaging.network.protocol.impl.tak.codec.TakProtobufCodec;
import io.mapsmessaging.network.protocol.transformation.cloudevent.CloudEventEnvelopeTransformation;
import io.mapsmessaging.network.protocol.transformation.cloudevent.CloudEventJsonTransformation;
import io.mapsmessaging.network.protocol.transformation.cloudevent.CloudEventNativeTransformation;
import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TakToCloudEventTransformationTest extends BaseTestConfig {

  private final SchemaManager schemaManager = SchemaManager.getInstance();
  private UUID protobufSchemaId;

  @AfterEach
  void cleanUpSchema() {
    if (protobufSchemaId != null) {
      schemaManager.removeSchema(protobufSchemaId.toString());
      protobufSchemaId = null;
    }
  }

  @Test
  void cotXmlDecodedMessageTransformsToCloudEventJsonData() throws Exception {
    CotXmlCodec codec = new CotXmlCodec();
    byte[] cotPayload = """
        <event uid="u-xml-1" type="a-f-G-U-C" time="2026-02-19T09:10:00Z" start="2026-02-19T09:10:00Z" stale="2026-02-19T09:15:00Z" how="m-g">
          <point lat="51.5007" lon="-0.1246" hae="35.0" ce="10.0" le="15.0"/>
        </event>
        """.getBytes(StandardCharsets.UTF_8);
    Message takMessage = codec.decode(cotPayload);

    Message cloudEventMessage = new CloudEventJsonTransformation().outgoing(takMessage, "/tak/cot/in");
    assertNotNull(cloudEventMessage);
    JsonObject cloudEvent = JsonParser.parseString(new String(cloudEventMessage.getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();

    assertEquals("1.0", cloudEvent.get("specversion").getAsString());
    assertEquals("application/json", cloudEvent.get("datacontenttype").getAsString());
    assertTrue(cloudEvent.has("data"));
    assertFalse(cloudEvent.getAsJsonObject("data").has("payload_base64"));
    assertEquals("u-xml-1", cloudEvent.get("mapsMeta_tak_uid").getAsString());
    assertEquals("xml", cloudEvent.get("mapsMeta_tak_format").getAsString());
  }

  @Test
  void takProtobufDecodedMessageTransformsToCloudEventJsonData() throws Exception {
    protobufSchemaId = UUID.randomUUID();
    ProtoBufSchemaConfig schema = (ProtoBufSchemaConfig) TestProtobufSchemas.protobufSchemaConfig(protobufSchemaId);
    schemaManager.addSchema("/tak/proto", schema);
    String descriptorBase64 = Base64.getEncoder().encodeToString(schema.getProtobufConfig().getDescriptorValue());
    String messageName = schema.getProtobufConfig().getMessageName();

    TakProtobufCodec codec = TakProtobufCodec.withSchemaFormatter(
        new CotXmlCodec(),
        1024 * 1024,
        descriptorBase64,
        messageName,
        protobufSchemaId.toString());
    Message takMessage = codec.decode(TestProtobufSchemas.encodeProtobufPayload());

    Message cloudEventMessage = new CloudEventJsonTransformation().outgoing(takMessage, "/tak/proto/in");
    assertNotNull(cloudEventMessage);
    JsonObject cloudEvent = JsonParser.parseString(new String(cloudEventMessage.getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();
    JsonObject data = cloudEvent.getAsJsonObject("data");

    assertEquals("1.0", cloudEvent.get("specversion").getAsString());
    assertEquals("application/json", cloudEvent.get("datacontenttype").getAsString());
    assertTrue(cloudEvent.has("mapsSchemaId"));
    assertTrue(data.has("id"));
    assertEquals("abc", data.get("id").getAsString());
    assertEquals(123, data.get("count").getAsInt());
    assertTrue(data.get("active").getAsBoolean());
    assertEquals("protobuf", cloudEvent.get("mapsMeta_tak_format").getAsString());
  }

  @Test
  void cotXmlRoundTripsTakCloudEventTakViaEnvelope() throws Exception {
    CotXmlCodec codec = new CotXmlCodec();
    byte[] cotPayload = "<event uid=\"u-xml-2\" type=\"a-f-G-U-C\" time=\"2026-02-19T09:10:00Z\" start=\"2026-02-19T09:10:00Z\" stale=\"2026-02-19T09:15:00Z\" how=\"m-g\"><point lat=\"1\" lon=\"2\" hae=\"3\" ce=\"4\" le=\"5\"/></event>"
        .getBytes(StandardCharsets.UTF_8);
    Message takMessage = codec.decode(cotPayload);
    Message cloudEvent = new CloudEventEnvelopeTransformation().outgoing(takMessage, "/tak/cot/in");

    byte[] encodedBackToTak = codec.encode(cloudEvent);
    assertArrayEquals(cotPayload, encodedBackToTak);

    Message decodedBack = codec.decode(encodedBackToTak);
    assertEquals("u-xml-2", decodedBack.getMeta().get("tak.uid"));
    assertEquals("xml", decodedBack.getMeta().get("tak.format"));
  }

  @Test
  void protobufRoundTripsTakCloudEventTakViaNative() throws Exception {
    protobufSchemaId = UUID.randomUUID();
    ProtoBufSchemaConfig schema = (ProtoBufSchemaConfig) TestProtobufSchemas.protobufSchemaConfig(protobufSchemaId);
    schemaManager.addSchema("/tak/proto", schema);
    String descriptorBase64 = Base64.getEncoder().encodeToString(schema.getProtobufConfig().getDescriptorValue());
    String messageName = schema.getProtobufConfig().getMessageName();

    TakProtobufCodec codec = TakProtobufCodec.withSchemaFormatter(
        new CotXmlCodec(),
        1024 * 1024,
        descriptorBase64,
        messageName,
        protobufSchemaId.toString());
    byte[] protobufPayload = TestProtobufSchemas.encodeProtobufPayload();
    Message takMessage = codec.decode(protobufPayload);
    Message cloudEvent = new CloudEventNativeTransformation().outgoing(takMessage, "/tak/proto/in");

    byte[] encodedBackToTak = codec.encode(cloudEvent);
    assertArrayEquals(protobufPayload, encodedBackToTak);

    Message decodedBack = codec.decode(encodedBackToTak);
    assertEquals("protobuf", decodedBack.getMeta().get("tak.format"));
  }

  @Test
  void cotXmlToCloudEventToTakProtobufToCloudEventToCotXml() throws Exception {
    protobufSchemaId = UUID.randomUUID();
    ProtoBufSchemaConfig schema = (ProtoBufSchemaConfig) TestProtobufSchemas.protobufSchemaConfig(protobufSchemaId);
    schemaManager.addSchema("/tak/proto", schema);
    String descriptorBase64 = Base64.getEncoder().encodeToString(schema.getProtobufConfig().getDescriptorValue());
    String messageName = schema.getProtobufConfig().getMessageName();

    CotXmlCodec xmlCodec = new CotXmlCodec();
    TakProtobufCodec protobufCodec = TakProtobufCodec.withSchemaFormatter(
        new CotXmlCodec(),
        1024 * 1024,
        descriptorBase64,
        messageName,
        protobufSchemaId.toString());

    byte[] startXml = "<event uid=\"u-chain-1\" type=\"a-f-G-U-C\" time=\"2026-02-19T09:10:00Z\" start=\"2026-02-19T09:10:00Z\" stale=\"2026-02-19T09:15:00Z\" how=\"m-g\"><point lat=\"10.1\" lon=\"11.2\" hae=\"12.3\" ce=\"13.4\" le=\"14.5\"/></event>"
        .getBytes(StandardCharsets.UTF_8);
    Message takXmlMessage = xmlCodec.decode(startXml);

    Message cloudEventFromXml = new CloudEventEnvelopeTransformation().outgoing(takXmlMessage, "/tak/cot/in");
    byte[] takProtobufBytes = protobufCodec.encode(cloudEventFromXml);
    Message takProtobufMessage = protobufCodec.decode(takProtobufBytes);

    Message cloudEventFromProtobuf = new CloudEventNativeTransformation().outgoing(takProtobufMessage, "/tak/proto/in");
    byte[] endXml = xmlCodec.encode(cloudEventFromProtobuf);
    Message decodedEndXml = xmlCodec.decode(endXml);

    assertEquals("u-chain-1", decodedEndXml.getMeta().get("tak.uid"));
    assertEquals("a-f-G-U-C", decodedEndXml.getMeta().get("tak.type"));
    assertEquals("10.1", decodedEndXml.getMeta().get("tak.lat"));
    assertEquals("11.2", decodedEndXml.getMeta().get("tak.lon"));
    assertEquals("xml", decodedEndXml.getMeta().get("tak.format"));
  }
}
