/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import static io.mapsmessaging.logging.ServerLogMessages.COT_PROTOCOL_OUTBOUND_SEND_FAILED;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.cot.CotStreamDecoder;
import io.mapsmessaging.cot.CotStreamEncoder;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.CotProtocolInformation;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.security.auth.Subject;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class CotProtocol extends Protocol {

  private static final Logger LOGGER = LoggerFactory.getLogger(CotProtocol.class);

  private final CotConfigDTO cotConfig;
  private final CotStreamDecoder decoder;
  private final CotStreamEncoder encoder;
  private final SelectorTask selectorTask;
  private final Session session;
  private final String inboundTopicName;
  private final String outboundTopicName;
  private final QualityOfService qos;

  public CotProtocol(EndPoint endPoint, Packet initialPacket) throws IOException {
    super(endPoint, endPoint.getConfig().getProtocolConfig("cot"));
    cotConfig = (CotConfigDTO) protocolConfig;
    decoder = new CotStreamDecoder(cotConfig.getMaximumEventSize());
    encoder = new CotStreamEncoder();
    qos = QualityOfService.getInstance(cotConfig.getQualityOfService());
    inboundTopicName = resolveTopic(cotConfig.getInboundTopicName());
    outboundTopicName = resolveTopic(cotConfig.getOutboundTopicName());
    try {
      session = buildSession("cot_" + endPoint.getName() + "_" + endPoint.getId(),
          cotConfig.getMaximumSessionExpiry());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while creating CoT session", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException("Unable to create CoT session", e);
    }
    if (outboundTopicName != null && !outboundTopicName.isBlank()) {
      SubscriptionContextBuilder builder =
          new SubscriptionContextBuilder(outboundTopicName, ClientAcknowledgement.AUTO);
      builder.setQos(qos);
      builder.setReceiveMaximum(10);
      builder.setNoLocalMessages(true);
      session.addSubscription(builder.build());
    }
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    if (initialPacket != null) {
      connect(null, null, null);
      processPacket(initialPacket);
    } else {
      selectorTask.register(SelectionKey.OP_READ);
    }
  }

  @Override
  public void connect(String sessionId, String username, String password) {
    setConnected(true);
    completedConnection();
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    byte[] bytes = new byte[packet.available()];
    packet.get(bytes);
    List<byte[]> events = decoder.accept(bytes);
    for (byte[] event : events) {
      publishInbound(event);
      receivedMessage();
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private void publishInbound(byte[] xml) throws IOException {
    if (inboundTopicName == null || inboundTopicName.isBlank()) {
      return;
    }
    Map<String, String> metadata = new HashMap<>();
    metadata.put("protocol", "CoT");
    metadata.put("version", "2.0");
    metadata.put("sessionId", session.getName());
    Message message = new MessageBuilder()
        .setOpaqueData(xml)
        .setContentType("text/xml")
        .setSchemaId(SchemaManager.DEFAULT_XML_SCHEMA.toString())
        .setQoS(qos)
        .setRetain(false)
        .setResponseTopic(outboundTopicName)
        .storeOffline(cotConfig.isStoreOffline())
        .setMeta(metadata)
        .build();
    try {
      Destination destination = session.findDestination(inboundTopicName, DestinationType.TOPIC).get();
      destination.storeMessage(message);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while publishing CoT event", e);
    } catch (ExecutionException e) {
      throw new IOException("Unable to publish CoT event", e);
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    try {
      byte[] xml = messageEvent.getMessage().getOpaqueData();
      if (xml == null || xml.length == 0) {
        return;
      }
      byte[] encoded = encoder.encode(xml);
      byte[] output = encoded;
      if (cotConfig.isAppendNewLine() && encoded[encoded.length - 1] != '\n') {
        output = Arrays.copyOf(encoded, encoded.length + 1);
        output[output.length - 1] = '\n';
      }
      endPoint.sendPacket(new Packet(ByteBuffer.wrap(output)));
      sentMessage();
    } catch (IOException exception) {
      LOGGER.log(
          COT_PROTOCOL_OUTBOUND_SEND_FAILED,
          exception,
          endPoint.getName(),
          exception.getMessage());
    } finally {
      if (messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
    }
  }

  @Override
  public void close() throws IOException {
    if (!session.isClosed()) {
      SessionManager.getInstance().close(session, false);
    }
    super.close();
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    CotProtocolInformation information = new CotProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

  @Override
  public String getName() {
    return "cot";
  }

  @Override
  public String getSessionId() {
    return session.getName();
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  private String resolveTopic(String topic) {
    return topic == null ? null : topic.replace("{interfaceName}", endPoint.getConfig().getName());
  }
}
