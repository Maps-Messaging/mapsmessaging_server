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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class JsonQueryTransformation implements InterServerTransformation {

  private static final String QUERY_PROPERTY = "query";

  private final Function<JsonElement, JsonElement> program;

  private Map<String, MessageFormatter> schemaMap = new ConcurrentHashMap<>();

  public JsonQueryTransformation() {
    this.program = null;

  }

  public JsonQueryTransformation(Function<JsonElement, JsonElement> program) {
    this.program = program;
  }

  @Override
  public String getName() {
    return "JsonQuery";
  }

  @Override
  public String getDescription() {
    return "Runs JsonQuery over incoming messages";
  }


  @Override
  public Protocol.ParsedMessage transform(String source, Protocol.ParsedMessage message) {
    if (program == null) {
      return message;
    }
    MessageFormatter messageFormatter = locateMessageFormatter(source, message);
    MessageBuilder messageBuilder = new MessageBuilder(message.getMessage());
    JsonObject jsonObject = null;
    if(messageFormatter != null) {
      try {
        jsonObject = messageFormatter.parseToJson(message.getMessage().getOpaqueData());
      } catch (IOException e) {
        // ToDo: log this
      }
    }
    else{
      jsonObject = JsonParser.parseString(new String(message.getMessage().getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    JsonElement element  = program.apply(jsonObject);
    if(element != null && !element.isJsonNull()) {
      messageBuilder.setOpaqueData(element.toString().getBytes(StandardCharsets.UTF_8));
      message.setMessage(messageBuilder.build());
      return message;
    }
    return null;
  }

  private MessageFormatter locateMessageFormatter(String source, Protocol.ParsedMessage message){
    MessageFormatter messageFormatter = schemaMap.get(source);
    if(messageFormatter == null) {
      String schemaId = message.getMessage().getSchemaId();
      if (schemaId == null) { // OK schemaId has not been set, this could mean an incoming event, lets use source and see
        try {
          DestinationImpl destination = MessageDaemon.getInstance().getDestinationManager().find(source).get(5, TimeUnit.SECONDS);
          schemaId = destination.getSchema().getUniqueId();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          // ToDo Log
        } catch (TimeoutException e) {
          //  ignore, we fail through
        }
      }
      if (schemaId != null) {
        SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
        try {
          messageFormatter = MessageFormatterFactory.getInstance().getFormatter(config);
          schemaMap.put(source, messageFormatter);
        } catch (IOException e) {
          // ToDo Log this
        }
      }
    }
    return messageFormatter;
  }

  @Override
  public InterServerTransformation build(ConfigurationProperties properties) {
    String queryText = properties.getProperty(QUERY_PROPERTY, null);
    if (queryText == null || queryText.isBlank()) {
      return new JsonQueryTransformation(data -> data);
    }

    JsonElement queryAst = JsonParser.parseString(queryText);

    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();
    Function<JsonElement, JsonElement> compiled = compiler.compile(queryAst);
    return new JsonQueryTransformation(compiled);
  }
}