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
import io.mapsmessaging.jsonquery.JsonQueryParser;
import io.mapsmessaging.jsonquery.parser.JsonQueryParseException;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
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

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class JsonQueryTransformation implements InterServerTransformation {

  private static final String QUERY_PROPERTY = "query";


  private final Logger logger = LoggerFactory.getLogger(JsonQueryTransformation.class);
  private final Function<JsonElement, JsonElement> program;

  protected final Map<String, MessageFormatter> schemaMap;
  private final String jsonQuery;

  public JsonQueryTransformation() {
    this(null, new ConcurrentHashMap<>(), "");

  }

  public JsonQueryTransformation(Function<JsonElement, JsonElement> program ) {
    this(program, new ConcurrentHashMap<>(), "");
  }

  JsonQueryTransformation(Function<JsonElement, JsonElement> program, Map<String, MessageFormatter> schemaMap, String jsonQuery) {
    this.program = program;
    this.schemaMap = schemaMap;
    this.jsonQuery = jsonQuery;
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

    JsonObject jsonObject;
    if (messageFormatter != null) {
      try {
        jsonObject = messageFormatter.parseToJson(message.getMessage().getOpaqueData());
      } catch (IOException e) {
        logger.log(JSON_QUERY_EXECUTION_EXCEPTION, jsonQuery, e.getMessage(), e);
        return message; // fail safe: don't drop on formatter failure
      }
    } else {
      jsonObject = JsonParser.parseString(
          new String(message.getMessage().getOpaqueData(), StandardCharsets.UTF_8)
      ).getAsJsonObject();
    }

    JsonElement element = program.apply(jsonObject);
    if (element != null && !element.isJsonNull()) {
      MessageBuilder messageBuilder = new MessageBuilder(message.getMessage());
      messageBuilder.setOpaqueData(element.toString().getBytes(StandardCharsets.UTF_8));
      message.setMessage(messageBuilder.build());
      return message;
    }
    return null; // drop
  }

  private MessageFormatter locateMessageFormatter(String source, Protocol.ParsedMessage message) {
    MessageFormatter messageFormatter = schemaMap.get(source);
    if (messageFormatter == null) {
      String schemaId = message.getMessage().getSchemaId();
      if (schemaId == null) {
        try {
          DestinationImpl destination = MessageDaemon.getInstance()
              .getDestinationManager()
              .find(source)
              .get(5, TimeUnit.SECONDS);
          schemaId = destination.getSchema().getUniqueId();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          logger.log(JSON_QUERY_EXECUTION_EXCEPTION, jsonQuery, e.getMessage(), e);
        } catch (TimeoutException e) {
          // ignore
        }
      }

      if (schemaId != null) {
        SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
        try {
          messageFormatter = MessageFormatterFactory.getInstance().getFormatter(config);
          if (messageFormatter != null) {
            schemaMap.put(source, messageFormatter);
          }
        } catch (IOException e) {
          logger.log(JSON_QUERY_EXECUTION_EXCEPTION, jsonQuery, e.getMessage(), e);
        }
      }
    }
    return messageFormatter;
  }

  @Override
  public InterServerTransformation build(ConfigurationProperties properties) {
    String queryText = properties != null ? properties.getProperty(QUERY_PROPERTY, null) : null;
    if (queryText == null || queryText.isBlank()) {
      return new JsonQueryTransformation(null); // true no-op, avoids re-encoding payload
    }

    try {
      JsonElement queryAst = parseQueryToAst(queryText);

      JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();
      Function<JsonElement, JsonElement> compiled = compiler.compile(queryAst);
      return new JsonQueryTransformation(compiled, new ConcurrentHashMap<>(), queryText);
    }
    catch(Throwable th){
      logger.log(JSON_QUERY_COMPILE_EXCEPTION, queryText, th.getMessage(), th);
    }
    return this;
  }

  private JsonElement parseQueryToAst(String queryText) {
    String trimmed = queryText.trim();
    try {
      JsonElement element = JsonParser.parseString(trimmed);
      if (looksLikeJsonQueryAst(element)) {
        return element;
      }
    } catch (Exception ignored) {
      // fall through
    }

    JsonElement ast = null;
    try {
      ast = JsonQueryParser.parse(trimmed);
      logger.log(JSON_QUERY_COMPILE_SUCCESS, trimmed, ast.toString());
    } catch (JsonQueryParseException e) {
      logger.log(JSON_QUERY_COMPILE_EXCEPTION, trimmed, e.getMessage(), e);
    }
    if (!looksLikeJsonQueryAst(ast)) {
      throw new IllegalArgumentException("Text query did not produce a JsonQuery AST: " + queryText);
    }
    return ast;
  }

  private boolean looksLikeJsonQueryAst(JsonElement element) {
    if (element == null || !element.isJsonArray()) {
      return false;
    }
    if (element.getAsJsonArray().isEmpty()) {
      return false;
    }
    JsonElement head = element.getAsJsonArray().get(0);
    return head.isJsonPrimitive() && head.getAsJsonPrimitive().isString();
  }

}
