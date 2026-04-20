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
import com.google.gson.JsonParser;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.canbus.j1939.CanId;
import io.mapsmessaging.canbus.j1939.CanIdBuilder;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.N2KConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.N2kProtocolInformation;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.canbus.CanbusEndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import  io.mapsmessaging.canbus.j1939.n2k.framing.FramePacker;
import io.mapsmessaging.network.protocol.impl.n2k.msg.source.AbstractAisFieldValueSource;
import io.mapsmessaging.network.protocol.impl.n2k.msg.source.ConfigurationInformationFieldValueSource;
import io.mapsmessaging.network.protocol.impl.n2k.msg.source.IsoAddressClaimFieldValueSource;
import io.mapsmessaging.network.protocol.impl.n2k.msg.source.ProductInformationFieldValueSource;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CanbusSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;

import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.impl.CanbusFormatter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.mapsmessaging.engine.schema.SchemaManager.DEFAULT_JSON_SCHEMA;
import static io.mapsmessaging.logging.ServerLogMessages.*;

public class N2kProtocol extends Protocol {
  private final Logger logger = LoggerFactory.getLogger(N2kProtocol.class);
  private final MessageFormatter formatter;
  private final FramePacker framePacker;
  private final Session session;
  private final InboundProcessor inboundProcessor;
  private final String topicTemplate;
  private final String rawTopicTemplate;
  private final boolean parseToJson;
  private final SchemaConfig defaultSchemaConfig = SchemaManager.getInstance().getSchema(DEFAULT_JSON_SCHEMA);
  private final DroneMonitor droneMonitor;
  private final int canbusAddress;

  public N2kProtocol(CanbusEndPoint endPoint, @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws IOException {
    super(endPoint, protocolConfig );
    CanbusSchemaConfig canbusSchema = new CanbusSchemaConfig();
    try {

      String databasePath = ((N2KConfigDTO)protocolConfig).getDatabasePath();
      String encodedDatabase = ((N2KConfigDTO)protocolConfig).getBase64EncodedDatabase();
      if(databasePath != null && !databasePath.isEmpty()) {
        logger.log(N2K_LOADING_DATABASE_FROM_FILE, databasePath);
        canbusSchema.setXmlPath(databasePath);
      }
      else if(encodedDatabase != null && !encodedDatabase.isEmpty()) {
        logger.log(N2K_LOADING_DATABASE_FROM_XML_DEFINITION);
        canbusSchema.setXmlBase64(Base64.getDecoder().decode(encodedDatabase));
      }
      else {
        logger.log(N2K_LOADING_DEFAULT_DATABASE, databasePath);
      }
    } catch (Exception e) {
      logger.log(N2K_LOADING_FAILED, e);
      throw new IOException(e);
    }
    N2KConfigDTO n2kConfig = (N2KConfigDTO)protocolConfig;
    topicTemplate = n2kConfig.getTopicNameTemplate();
    rawTopicTemplate =  n2kConfig.getUnknownPacketTopic().replace("{candevice}",endPoint.getName());
    parseToJson =n2kConfig.isParseToJson();
    canbusAddress = n2kConfig.getCanBusAddress();
    String inboundTopicName =n2kConfig.getInboundTopicName();

    formatter = MessageFormatterFactory.getInstance().getFormatter(canbusSchema);
    framePacker = new FramePacker( ((CanbusFormatter)formatter).getParser() );
    try {
      session = buildSession(endPoint.getName(), 10000);
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e);
    }
    if(inboundTopicName != null && !inboundTopicName.isEmpty()){
      SubscriptionContextBuilder scb = new SubscriptionContextBuilder(inboundTopicName, ClientAcknowledgement.AUTO);
      SubscriptionContext context = scb.setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(10)
          .setNoLocalMessages(true)
          .build();
      session.addSubscription(context);
    }
    inboundProcessor  = new InboundProcessor(this);
    Thread t = new Thread(inboundProcessor);
    t.start();
    logger.log(N2K_PROTOCOL_CREATED_AND_BOUND,endPoint.getName());
    if(((N2KConfigDTO)protocolConfig).isPublishMavlinkDrones()){
      formatAndSend(IsoAddressClaimFieldValueSource.PGN, 0xff, new IsoAddressClaimFieldValueSource(MessageDaemon.getInstance().getUuid().toString()));
      droneMonitor = new DroneMonitor(this, ((CanbusFormatter)formatter).getParser());
      MessageDaemon.getInstance().getSubSystemManager().getTwinManager().addObserver(droneMonitor);
    }
    else{
      droneMonitor = null;
    }
  }

  @Override
  public void close() throws IOException {
    super.close();
    inboundProcessor.close();
    endPoint.close();
    logger.log(N2K_PROTOCOL_CLOSING, endPoint.getName());
    droneMonitor.close();
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
    Message msg = messageEvent.getMessage();
    byte[] payload = msg.getOpaqueData();
    CanFrame frame = null;
    // Let's try raw
    if(payload != null && payload.length == 13) {
      frame = CanFrame.fromBytes(payload);
    }

    // Lets try json
    if(frame == null && payload != null) {
      String json = new String(payload);
      JsonObject element = JsonParser.parseString(json).getAsJsonObject();
      try {
        byte[] data = formatter.parseFromJson(element);
        frame = CanFrame.fromBytes(data);
      } catch (IOException e) {
        logger.log(N2K_PROTOCOL_CANBUS_BUILD_ERROR, e);
      }
    }
    if(frame != null) {
      try {
        ((CanbusEndPoint) endPoint).writeFrame(frame);
      } catch (IOException e) {
        logger.log(N2K_PROTOCOL_CANBUS_BUILD_ERROR, e);
      }
    }
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    CanFrame frame = ((CanbusEndPoint) endPoint).readFrame();
    if(logger.isDebugEnabled() && frame != null){
      logger.log(N2K_PROTOCOL_PARSING_PACKET, packetToString(frame));
    }
    if(frame != null) {
      if(parseToJson) {
        try {
          processPacket(formatter.parseToJson(frame.getRawData(), SchemaManager.getInstance().getDefaultParseMode()));
        } catch (ParseException e) {
          e.printStackTrace();
        }
      }
      else{
        Map<String, String> metaData = new HashMap<>();
        metaData.put("protocol", "n2k");
        metaData.put("version", getVersion());
        metaData.put("sessionId", session.getName());
        metaData.put("time_ms", "" + System.currentTimeMillis());
        byte[] data = frame.getRawData();
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setOpaqueData(data)
            .setQoS(QualityOfService.AT_MOST_ONCE)
            .setMeta(metaData)
            .setRetain(false)
            .setContentType("canbus");

        Message message = messageBuilder.build();
        publishMessage(rawTopicTemplate, message);
      }
      handleInboundRequest(frame);
    }
    return true;
  }

  private void handleInboundRequest(CanFrame frame) throws IOException {
    int id = frame.canIdentifier();
    CanId canId = CanId.parse(id);
    int pgn = canId.getPgn();
    if(pgn == 59904){
      byte[] payload = frame.data();
      int requestedPgn = (payload[0] & 0xFF) | ((payload[1] & 0xFF) << 8) | ((payload[2] & 0xFF) << 16);
      AbstractAisFieldValueSource response = switch (requestedPgn) {
        case 126996 -> new ProductInformationFieldValueSource("Maps Messaging Server", BuildInfo.getBuildVersion(), null, null);
        case 126998 -> new ConfigurationInformationFieldValueSource();
        case 60928 -> new IsoAddressClaimFieldValueSource(MessageDaemon.getInstance().getUuid().toString());
        default -> null;
      };
      if(response != null){
        byte[] data = ((CanbusFormatter)formatter).getParser().encodeFromSource(requestedPgn, response);
        writePgn(requestedPgn, canId.getSourceAddress(), data);
      }
      else{
        System.err.println("Unknown PGN: " + requestedPgn);
      }
    }
  }

  protected void formatAndSend(int pgn, int destinationAddress, AbstractAisFieldValueSource response) throws IOException {
    byte[] data = ((CanbusFormatter)formatter).getParser().encodeFromSource(pgn, response);
    writePgn(pgn, destinationAddress, data);
  }

  public void writePgn(int pgn, int destinationAddress, byte[] data) throws IOException {
    int canId1 = CanIdBuilder.build(pgn, 6, canbusAddress, destinationAddress);
    List<CanFrame> frames = framePacker.packFastPacket(pgn, canId1, canbusAddress, 0, data);
    for(CanFrame frame1 : frames) {
      ((CanbusEndPoint) getEndPoint()).writeFrame(frame1);
    }
  }

  public boolean processPacket(JsonObject json) {
    MessageBuilder messageBuilder = new MessageBuilder();
    Map<String, String> metaData = new HashMap<>();
    metaData.put("protocol", "n2k");
    metaData.put("version", getVersion());
    metaData.put("sessionId", session.getName());
    metaData.put("time_ms", "" + System.currentTimeMillis());
    Map<String, TypedData> map = new LinkedHashMap<>();

    int pgn = 0;
    String name = null;
    if(json.has("j1939")) {
      JsonObject j1939 = json.getAsJsonObject("j1939");
      pgn = j1939.get("pgn").getAsInt();
      map.put("pgn", new TypedData(pgn));
      if(j1939.has("n2k")){
        JsonObject n2k = j1939.getAsJsonObject("n2k");
        name = n2k.get("name").getAsString();
        map.put("name", new TypedData(name));
      }
    }

    Message message = messageBuilder.setContentType("n2k")
        .setOpaqueData(json.toString().getBytes())
        .setDataMap(map)
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .storeOffline(false)
        .setContentType("canbus")
        .setMeta(metaData)
        .setSchemaId(defaultSchemaConfig.getUniqueId())
        .build();
    String topicName = computeTopicName(pgn, name);
    publishMessage(topicName, message);
    return true;
  }
//"/{candevice}/{pgn}/{messageName}",
  protected String computeTopicName(int pgn, String messageName) {
    if(messageName == null) {
      return rawTopicTemplate;
    }
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
    return "CanId: " + frame.canIdentifier() + " " + "Data: " + Base64.getEncoder().encodeToString(frame.data());
  }

  private void publishMessage(String topicName, Message message) {
    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    if (future != null) {
      future.thenApply(destination -> {
        try {
          if(destination != null){
            if(destination.getSchema() == null || destination.getSchema().getUniqueId().equals(SchemaManager.DEFAULT_RAW_UUID.toString())){
              // set the schema here
              destination.updateSchema(defaultSchemaConfig, null );
            } else {
              destination.getSchema();
            }
            destination.storeMessage(message);
          }
        } catch (IOException e) {
          future.completeExceptionally(e);
        }
        return destination;
      });
    }
  }
}
