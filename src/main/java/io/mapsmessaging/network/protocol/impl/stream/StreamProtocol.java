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

package io.mapsmessaging.network.protocol.impl.stream;

import com.google.gson.JsonObject;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.StreamConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.StompProtocolInformation;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.stream.assemblers.Assembler;
import io.mapsmessaging.network.protocol.impl.stream.assemblers.JsonBracePacketAssembler;
import io.mapsmessaging.network.protocol.impl.stream.assemblers.LengthPrefixedPacketAssembler;
import io.mapsmessaging.network.protocol.impl.stream.assemblers.NoOpAssembler;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.channels.SelectionKey.OP_READ;

public class StreamProtocol extends Protocol implements CompleteHandler {

  @Getter
  private final Logger logger;
  private final SelectorTask selectorTask;
  private final MessageFormatter messageFormatter;
  private final Assembler assembler;

  private Session session;


  public StreamProtocol(EndPoint endPoint) throws IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("stream"));
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    logger = LoggerFactory.getLogger("Stream Protocol on " + endPoint.getName());
    logger.log(ServerLogMessages.STOMP_STARTING, endPoint.toString());
    StreamConfigDTO config = (StreamConfigDTO)protocolConfig;
    assembler = buildAssembler(config, this);
    SchemaConfig schemaConfig = SchemaManager.getInstance().getSchemaByName(config.getSchemaName());
    if(schemaConfig != null) {
      messageFormatter = SchemaManager.getInstance().getMessageFormatter(schemaConfig);
    }
    else{
      messageFormatter = null;
    }
    if(endPoint.isUDP()){
      registerRead();
    }
  }

  private static Assembler buildAssembler(StreamConfigDTO config, CompleteHandler handler){
    return switch (config.getAssemblyType()) {
      case JSON -> new JsonBracePacketAssembler(config.getMaxBufferSize(), true, handler);
      case LENGTH_PREFIX -> new LengthPrefixedPacketAssembler(config.getLengthFieldSize(), config.getMaxBufferSize(), true, config.getEndianness(), handler);
      default -> new NoOpAssembler(handler);
    };
  }

  public StreamProtocol(EndPoint endPoint, Packet packet) throws IOException {
    this(endPoint);
    processPacket(packet);
  }

  @Override
  public void close() {
    logger.log(ServerLogMessages.STOMP_CLOSING, endPoint.toString());
    try {
      super.close();
      endPoint.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
  }

  @Override
  public Subject getSubject() {
    return session != null ? session.getSecurityContext().getSubject() : null;
  }

  @Override
  public void setSession(Session session) {
    this.session = session;
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable ParserExecutor executor, @Nullable InterServerTransformation transformer, StatisticsConfigDTO statistics) throws IOException {
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, String selector, @Nullable InterServerTransformation transformer, @Nullable NamespaceFilters namespaceFilters, StatisticsConfigDTO statistics) throws IOException {

  }

  @Override
  public String getSessionId() {
    return session!= null?session.getName() : "unknown";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  public String getName() {
    return "STREAM";
  }


  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    messageEvent.getCompletionTask().run();
  }

  // <editor-fold desc="Read Frame functions">
  public void registerRead() throws IOException {
    selectorTask.register(OP_READ);
  }

  public boolean processPacket(Packet packet) throws IOException {
    assembler.processPacket(packet);
    registerRead();
    return true;
  }

  @Override
  public void sendKeepAlive() {
    // No Op, nothing to do
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    StompProtocolInformation information = new StompProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  @Override
  public void completePacket(ByteBuffer buffer) {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    System.err.println("Packet received: "+new String(bytes));
    if(messageFormatter != null) {
      try {
        JsonObject jsonObject = messageFormatter.parseToJson(bytes);
        messageFormatter.parse(bytes);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    else{
      // Binary so no processing pass on
    }
  }
}
