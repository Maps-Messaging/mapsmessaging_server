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
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.BeforeEach;

import static io.mapsmessaging.api.transformers.TransformationTestSupport.mockMessage;
import static io.mapsmessaging.api.transformers.TransformationTestSupport.parsedMessage;
import static io.mapsmessaging.engine.schema.SchemaManager.DEFAULT_JSON_SCHEMA;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractInterServerTransformationTest extends BaseTestConfig {

  public static final SchemaConfig JSON_SCHEMA_CONFIG = buildSchema();
  protected static final String SOURCE = "/test/source";
  protected static final String DESTINATION = "/test/destination";
  protected InterServerTransformation transformer;

  private static SchemaConfig buildSchema() {
    return SchemaManager.getInstance().getSchema(DEFAULT_JSON_SCHEMA);
  }

  protected abstract InterServerTransformation createTransformer();

  @BeforeEach
  void setupTransformer() {
    transformer = createTransformer();
    assertNotNull(transformer, "createTransformer() must not return null");
  }

  protected Protocol.ParsedMessage transform(byte[] opaqueData) {
    Message message = mockMessage(opaqueData, DEFAULT_JSON_SCHEMA.toString());
    Protocol.ParsedMessage parsedMessage = parsedMessage(DESTINATION, message);
    return assertDoesNotThrow(() -> transformer.transform(SOURCE, parsedMessage));
  }

  protected Protocol.ParsedMessage transform(byte[] opaqueData, String schemaId) {
    Message message = mockMessage(opaqueData, schemaId);
    message.setSchemaId(DEFAULT_JSON_SCHEMA.toString());
    Protocol.ParsedMessage parsedMessage = parsedMessage(DESTINATION, message);
    return assertDoesNotThrow(() -> transformer.transform(SOURCE, parsedMessage));
  }

  protected Protocol.ParsedMessage transformWithParsed(Protocol.ParsedMessage parsedMessage) {
    return assertDoesNotThrow(() -> transformer.transform(SOURCE, parsedMessage));
  }
}
