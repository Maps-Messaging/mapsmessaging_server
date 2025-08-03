package io.mapsmessaging.ml.streams;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.config.ml.LlmConfigDTO;
import io.mapsmessaging.dto.rest.config.ml.MLEventStreamDTO;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.ml.llm.ChatGPTSelectorClient;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;

public class StreamHandler implements ClientConnection, MessageListener {

  private final MLEventStreamDTO eventStream;
  private SubscribedEventManager subscribedEventManager;
  private ParserExecutor parserExecutor;
  private Session session;

  public StreamHandler(MLEventStreamDTO eventStream, LlmConfigDTO llmConfigDTO) {
    ChatGPTSelectorClient client = new ChatGPTSelectorClient(llmConfigDTO.getModel(), llmConfigDTO.getApiToken());
    String hint = "This schema represents structured event data. Use only numeric fields that represent live data for anomaly detection. Do not use timestamps, status flags, or identifiers.\n";
    this.eventStream = eventStream;
    try {
      String selector = client.generateSelector(getSchema(), hint);
      parserExecutor = SelectorParser.compile(selector);
    } catch (ParseException e) {
      e.printStackTrace();
      // log this
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    try {
      session = createSession(eventStream);
    } catch (LoginException e) {
      // log this
    } catch (IOException e) {
      // log this
    }
  }

  public void start() {
    try {
      session.start();
      SubscriptionContextBuilder scb = new SubscriptionContextBuilder(eventStream.getTopicFilter(), ClientAcknowledgement.AUTO);
      scb.setMode(DestinationMode.NORMAL)
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setSelector(eventStream.getSelector())
          .setAlias(eventStream.getId());
      subscribedEventManager = session.addSubscription(scb.build());
    } catch (IOException e) {
      // log this
    }
  }

  public void stop() {
    try {
      session.removeSubscription(subscribedEventManager.getContext().getAlias());
      SessionManager.getInstance().close(session, true);
    } catch (IOException e) {
      // log this
    }
  }

  private Session createSession(MLEventStreamDTO eventStream) throws LoginException, IOException {
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder(eventStream.getId(), this);
    sessionContextBuilder.setPersistentSession(false)
        .setKeepAlive(0)
        .setSessionExpiry(0);
    return SessionManager.getInstance().create(sessionContextBuilder.build(), this);
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getName() {
    return eventStream.getId();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void sendKeepAlive() {
    // No Op, nothing to do here
  }

  @Override
  public Principal getPrincipal() {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  public String getUniqueName() {
    return eventStream.getId();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    System.err.println("Message Received: " + messageEvent.getMessage());
    if (parserExecutor.evaluate(messageEvent.getMessage())) {
      // send to outlier topic and process the outliers
    }
  }

  private String getSchema() {
    return "{\n" +
        "  \"schema\": {\n" +
        "    \"format\": \"JSON\",\n" +
        "    \"uuid\": \"0f9f2c92-c081-5b8a-ac30-1e39e0fc3a7a\",\n" +
        "    \"creation\": \"2025-08-01T16:02:18.140841707\",\n" +
        "    \"comments\": \"Air Quality Sensor for PM, RH/T, VOC, Nox, CO2, HCOH\",\n" +
        "    \"version\": \"1\",\n" +
        "    \"title\": \"SEN6x\",\n" +
        "    \"mime-type\": \"application/json\",\n" +
        "    \"resource-type\": \"sensor\",\n" +
        "    \"interface-description\": \"Returns Air Quality valies in JSON\",\n" +
        "    \"jsonSchema\": {\n" +
        "      \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
        "      \"title\": \"SEN6x\",\n" +
        "      \"description\": \"Air Quality Sensor for PM, RH/T, VOC, Nox, CO2, HCOH\",\n" +
        "      \"type\": \"object\",\n" +
        "      \"properties\": {\n" +
        "        \"DeviceStaticDataSchema\": {\n" +
        "          \"type\": \"object\"\n" +
        "        },\n" +
        "        \"SensorDataSchema\": {\n" +
        "          \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
        "          \"type\": \"object\",\n" +
        "          \"properties\": {\n" +
        "            \"timestamp\": {\n" +
        "              \"type\": \"string\",\n" +
        "              \"format\": \"date-time\",\n" +
        "              \"description\": \"Optional ISO 8601 UTC timestamp (e.g., 2025-05-29T07:28:15.123Z)\",\n" +
        "              \"readOnly\": true\n" +
        "            },\n" +
        "            \"Product Name\": {\n" +
        "              \"type\": \"string\",\n" +
        "              \"description\": \"Unit: \"\n" +
        "            },\n" +
        "            \"Serial Number\": {\n" +
        "              \"type\": \"string\",\n" +
        "              \"description\": \"Unit: \"\n" +
        "            },\n" +
        "            \"status\": {\n" +
        "              \"timestamp\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"format\": \"date-time\",\n" +
        "                \"description\": \"Optional ISO 8601 UTC timestamp (e.g., 2025-05-29T07:28:15.123Z)\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"Fan Error\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: Fan failure detected\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"RHT Error\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: Humidity/Temperature sensor error\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"Gas Error\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: Gas sensor failure\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"CO₂-2 Error\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: CO₂ sensor 2 failure\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"HCHO Error\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: Formaldehyde sensor error\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"PM Error\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: Particulate Matter sensor error\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"CO₂-1 Error\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: CO₂ sensor 1 failure\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"Speed Warning\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: Fan speed abnormal\",\n" +
        "                \"readOnly\": true\n" +
        "              },\n" +
        "              \"Compensation Active\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"description\": \"Unit: Compensation enabled\",\n" +
        "                \"readOnly\": true\n" +
        "              }\n" +
        "            },\n" +
        "            \"CO₂\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 400,\n" +
        "              \"maximum\": 5000,\n" +
        "              \"x-precision\": 0,\n" +
        "              \"description\": \"Unit: ppm\"\n" +
        "            },\n" +
        "            \"temperature\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": -10,\n" +
        "              \"maximum\": 60,\n" +
        "              \"x-precision\": 2,\n" +
        "              \"description\": \"Unit: °C\"\n" +
        "            },\n" +
        "            \"humidity\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 0,\n" +
        "              \"maximum\": 100,\n" +
        "              \"x-precision\": 1,\n" +
        "              \"description\": \"Unit: %\"\n" +
        "            },\n" +
        "            \"vocIndex\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 0,\n" +
        "              \"maximum\": 500,\n" +
        "              \"x-precision\": 0,\n" +
        "              \"description\": \"Unit: index\"\n" +
        "            },\n" +
        "            \"noxIndex\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 0,\n" +
        "              \"maximum\": 500,\n" +
        "              \"x-precision\": 0,\n" +
        "              \"description\": \"Unit: index\"\n" +
        "            },\n" +
        "            \"pm1_0\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 0,\n" +
        "              \"maximum\": 1000,\n" +
        "              \"x-precision\": 1,\n" +
        "              \"description\": \"Unit: µg/m³\"\n" +
        "            },\n" +
        "            \"pm2_5\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 0,\n" +
        "              \"maximum\": 1000,\n" +
        "              \"x-precision\": 1,\n" +
        "              \"description\": \"Unit: µg/m³\"\n" +
        "            },\n" +
        "            \"pm4_0\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 0,\n" +
        "              \"maximum\": 1000,\n" +
        "              \"x-precision\": 1,\n" +
        "              \"description\": \"Unit: µg/m³\"\n" +
        "            },\n" +
        "            \"pm10_0\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 0,\n" +
        "              \"maximum\": 1000,\n" +
        "              \"x-precision\": 1,\n" +
        "              \"description\": \"Unit: µg/m³\"\n" +
        "            },\n" +
        "            \"airQualityValue\": {\n" +
        "              \"type\": \"string\",\n" +
        "              \"description\": \"Unit: \"\n" +
        "            },\n" +
        "            \"airQualityIndex\": {\n" +
        "              \"type\": \"number\",\n" +
        "              \"minimum\": 0,\n" +
        "              \"maximum\": 500,\n" +
        "              \"x-precision\": 0,\n" +
        "              \"description\": \"Unit: AQI\"\n" +
        "            }\n" +
        "          },\n" +
        "          \"required\": [\n" +
        "            \"Product Name\",\n" +
        "            \"Serial Number\",\n" +
        "            \"status\",\n" +
        "            \"CO₂\",\n" +
        "            \"temperature\",\n" +
        "            \"humidity\",\n" +
        "            \"vocIndex\",\n" +
        "            \"noxIndex\",\n" +
        "            \"pm1_0\",\n" +
        "            \"pm2_5\",\n" +
        "            \"pm4_0\",\n" +
        "            \"pm10_0\",\n" +
        "            \"airQualityValue\",\n" +
        "            \"airQualityIndex\"\n" +
        "          ],\n" +
        "          \"additionalProperties\": false\n" +
        "        }\n" +
        "      },\n" +
        "      \"required\": [\n" +
        "        \"DeviceStaticDataSchema\",\n" +
        "        \"SensorDataSchema\"\n" +
        "      ],\n" +
        "      \"additionalProperties\": false\n" +
        "    }\n" +
        "  }\n" +
        "}";
  }
}
