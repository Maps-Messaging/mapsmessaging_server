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

package io.mapsmessaging.network.protocol.impl.n2k;

import com.google.gson.JsonObject;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.canbus.j1939.CanId;
import io.mapsmessaging.canbus.j1939.n2k.N2kParserFactory;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiledMessage;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiledRegistry;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiler;
import io.mapsmessaging.canbus.j1939.n2k.parser.N2kXmlDialectParser;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.N2KConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.N2kProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.canbus.CanbusEndPoint;
import io.mapsmessaging.network.protocol.Protocol;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class N2kProtocol extends Protocol {
  private final Logger logger = LoggerFactory.getLogger(N2kProtocol.class);
  private final N2kMessageParser parser;
  private final Session session;
  private final InboundProcessor inboundProcessor;
  private final String topicTemplate;
  private final String rawTopicTemplate;

  public N2kProtocol(CanbusEndPoint endPoint, @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws IOException {
    super(endPoint, protocolConfig );
    N2kCompiledRegistry registry;
    try {
      String databasePath = ((N2KConfigDTO)protocolConfig).getDatabasePath();
      if(databasePath != null && !databasePath.isEmpty()) {
        logger.log(N2K_LOADING_DATABASE_FROM_FILE, databasePath);
        registry = N2kCompiler.compile(N2kXmlDialectParser.parseFromFile(Path.of(databasePath)));
      }
      else {
        logger.log(N2K_LOADING_DATABASE_FROM_FILE, databasePath);
        registry = N2kParserFactory.getN2kParser();
      }
    } catch (Exception e) {
      logger.log(N2K_LOADING_FAILED, e);
      throw new IOException(e);
    }
    topicTemplate = ((N2KConfigDTO)protocolConfig).getTopicNameTemplate();
    rawTopicTemplate =  ((N2KConfigDTO)protocolConfig).getUnknownPacketTopic();
    parser = new N2kMessageParser(registry);
    try {
      session = buildSession(endPoint.getName(), 10000);
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e);
    }
    inboundProcessor  = new InboundProcessor(this);
    Thread t = new Thread(inboundProcessor);
    t.start();
    logger.log(N2K_PROTOCOL_CREATED_AND_BOUND,endPoint.getName());
  }

  public void close() throws IOException {
    super.close();
    inboundProcessor.close();
    endPoint.close();
    logger.log(N2K_PROTOCOL_CLOSING, endPoint.getName());
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    N2kProtocolInformation information = new N2kProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    CanFrame frame = ((CanbusEndPoint) endPoint).readFrame();
    if(logger.isDebugEnabled() && frame != null){
      logger.log(N2K_PROTOCOL_PARSING_PACKET, packetToString(frame));
    }
    if(frame != null) {
      int id = frame.getCanIdentifier();
      CanId canId = CanId.parse(id);
      N2kCompiledMessage compiledMessage = parser.getRegistry().getMessagesByPgn().get(canId.getPgn());
      if(compiledMessage != null) {
        if(logger.isDebugEnabled()){
          logger.log(N2K_PROTOCOL_PARSED_KNOWN_PACKET, canId.getPgn());
        }
        JsonObject json = parser.decodeToJson(canId.getPgn(), frame.getData());
        json = buildEnvelope(json, canId, frame);
        processPacket(compiledMessage.getId(), canId, json.toString().getBytes());
      }
      else{
        if(logger.isDebugEnabled()){
          logger.log(N2K_PROTOCOL_PARSED_UNKNOWN_PACKET, canId.getPgn());
        }
        processRawPacket(frame);
      }
    }
    return true;
  }

  private JsonObject buildEnvelope(JsonObject n2kJson, CanId canId, CanFrame frame) {
    JsonObject envelope = new JsonObject();
    envelope.addProperty("timestamp", System.currentTimeMillis());
    JsonObject j1939 = new JsonObject();
    j1939.addProperty("source", canId.getSourceAddress());
    if (canId.isPdu1()) {
      j1939.addProperty("destination", canId.getDestinationAddress());
    }
    j1939.addProperty("priority", canId.getPriority());
    j1939.addProperty("raw", Base64.getEncoder().encodeToString(frame.getData()));
    j1939.add("n2k",n2kJson);
    envelope.addProperty("protocol", getName());
    envelope.add("j1939", j1939);
    return envelope;
  }

  public boolean processRawPacket(CanFrame frame)  {
    MessageBuilder messageBuilder = new MessageBuilder();
    Map<String, String> metaData = new HashMap<>();
    metaData.put("protocol", "n2k");
    metaData.put("version", getVersion());
    metaData.put("sessionId", session.getName());
    metaData.put("time_ms", "" + System.currentTimeMillis());

    Map<String, TypedData> map = new LinkedHashMap<>();
    map.put("CanId", new TypedData(frame.getCanIdentifier() + ""));

    Message message = messageBuilder.setContentType("n2k")
        .setOpaqueData(frame.getData())
        .setDataMap(map)
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .storeOffline(false)
        .setMeta(metaData)
        .build();
    String topicName = rawTopicTemplate.replace("{candevice}",endPoint.getName());

    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    if (future != null) {
      future.thenApply(destination -> {
        try {
          destination.storeMessage(message);
        } catch (IOException e) {
          future.completeExceptionally(e);
        }
        return destination;
      });
    }
    return true;

  }

  public boolean processPacket(String name,  CanId canId, byte[] raw) {
    MessageBuilder messageBuilder = new MessageBuilder();
    Map<String, String> metaData = new HashMap<>();
    metaData.put("protocol", "n2k");
    metaData.put("version", getVersion());
    metaData.put("sessionId", session.getName());
    metaData.put("time_ms", "" + System.currentTimeMillis());

    Map<String, TypedData> map = new LinkedHashMap<>();
    map.put("name", new TypedData(name));
    map.put("pgn", new TypedData(canId.getPgn()));

    Message message = messageBuilder.setContentType("n2k")
        .setOpaqueData(raw)
        .setDataMap(map)
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .storeOffline(false)
        .setMeta(metaData)
        .build();

    String topicName = computeTopicName(canId.getPgn(), name);


    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    if (future != null) {
      future.thenApply(destination -> {
        try {
          destination.storeMessage(message);
        } catch (IOException e) {
          future.completeExceptionally(e);
        }
        return destination;
      });
    }
    return true;
  }
//"/{candevice}/{pgn}/{messageName}",
  protected String computeTopicName(int pgn, String messageName) {
    String template = topicTemplate;
    template = template.replace("{candevice}",endPoint.getName());
    template = template.replace("{pgn}", ""+pgn);
    template = template.replace("{messageName}", messageName);
    return template;
  }

  @Override
  public String getName() {
    return "n2k";
  }

  @Override
  public String getSessionId() {
    return session.getName();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  private String packetToString(CanFrame frame) {
    StringBuilder sb = new StringBuilder();
    sb.append("CanId: ").append(frame.getCanIdentifier()).append(" ");
    sb.append("Data: ").append(Base64.getEncoder().encodeToString(frame.getData()));
    return sb.toString();
  }

}
