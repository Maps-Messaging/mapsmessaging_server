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

import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.AvroSchemaConfig;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public final class TestAvroSchemas {

  public static final String SCHEMA_JSON =
      "{"
          + "\"type\":\"record\","
          + "\"name\":\"TestEvent\","
          + "\"namespace\":\"io.mapsmessaging.test\","
          + "\"fields\":["
          + "{\"name\":\"id\",\"type\":\"string\"},"
          + "{\"name\":\"count\",\"type\":\"int\"},"
          + "{\"name\":\"active\",\"type\":\"boolean\"}"
          + "]"
          + "}";

  private TestAvroSchemas() {
  }

  public static SchemaConfig avroSchemaConfig(UUID schemaId, String schemaJson) {
    AvroSchemaConfig config = new AvroSchemaConfig();
    config.setSchema(JsonParser.parseString(schemaJson).getAsJsonObject());
    config.setUniqueId(schemaId);
    return config;
  }

  public static byte[] encodeAvroPayload() {
    try {
      Schema schema = new Schema.Parser().parse(SCHEMA_JSON);

      GenericRecord record = new GenericData.Record(schema);
      record.put("id", "abc");
      record.put("count", 123);
      record.put("active", true);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);

      GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
      writer.write(record, encoder);
      encoder.flush();

      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
