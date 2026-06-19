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

package io.mapsmessaging.state.stanag.messages;

import com.google.gson.JsonObject;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TaskSchemaValidator {

  public void validate(String schemaName, JsonObject jsonObject) {
    SchemaConfig schemaConfig = SchemaManager.getInstance().getSchemaByName(schemaName);

    if (schemaConfig == null) {
      return;
    }

    try {
      MessageFormatter messageFormatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig, SchemaManager.getInstance());
      messageFormatter.parse(jsonObject.toString().getBytes(StandardCharsets.UTF_8), ParseMode.STRICT);
    } catch (IOException exception) {
      System.err.println("---------------------------------------------");
      System.err.println("Original:" + jsonObject);
      System.err.println("---------------------------------------------");
      exception.printStackTrace();
    }
  }
}