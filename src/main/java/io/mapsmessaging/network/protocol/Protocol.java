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

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.admin.ProtocolJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.Timeoutable;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilter;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Protocol implements SelectorCallback, MessageListener, Timeoutable {
  protected final EndPoint endPoint;

  @Getter
  protected final Map<String, Transformer> destinationTransformerMap;
  @Getter
  protected final Map<String, String> topicNameMapping;

  @Getter
  protected final Map<String, Analyser> topicNameAnalyserMap;

  @Getter
  protected final Map<String, StatisticsConfigDTO> resourceNameAnalyserMap;


  @Getter
  @Setter
  private NamespaceFilters namespaceFilters;


  @Getter
  protected final Map<String, ParserExecutor> parserLookup;
  protected final ProtocolJMX mbean;

  @Getter
  private boolean connected;

  @Setter
  @Getter
  protected long keepAlive;
  private boolean completed;

  @Getter
  @Setter
  protected ProtocolMessageTransformation transformation;

  @Getter
  protected final ProtocolConfigDTO protocolConfig;

  protected Protocol(@NonNull @NotNull EndPoint endPoint, @NotNull @NonNull ProtocolConfigDTO protocolConfig) {
    this.endPoint = endPoint;
    this.protocolConfig = protocolConfig;
    mbean = new ProtocolJMX(endPoint.getJMXTypePath(), this);
    connected = false;
    completed = false;
    destinationTransformerMap = new ConcurrentHashMap<>();
    parserLookup = new ConcurrentHashMap<>();
    topicNameMapping = new ConcurrentHashMap<>();
    topicNameAnalyserMap = new ConcurrentHashMap<>();
    resourceNameAnalyserMap = new ConcurrentHashMap<>();
    endPoint.setBoundProtocol(this);
  }

  protected Protocol(@NonNull @NotNull EndPoint endPoint, @NonNull @NotNull SocketAddress socketAddress, @NotNull @NonNull ProtocolConfigDTO protocolConfig) {
    this.endPoint = endPoint;
    this.protocolConfig = protocolConfig;
    String endPointName = socketAddress.toString();
    endPointName = endPointName.replace(":", "_");
    List<String> jmsList = new ArrayList<>(endPoint.getJMXTypePath());
    jmsList.add("remoteEndPoint=" + endPointName);

    mbean = new ProtocolJMX(jmsList, this);
    connected = false;
    completed = false;
    destinationTransformerMap = new ConcurrentHashMap<>();
    parserLookup = new ConcurrentHashMap<>();
    topicNameMapping = new ConcurrentHashMap<>();
    topicNameAnalyserMap = new ConcurrentHashMap<>();
    resourceNameAnalyserMap = new ConcurrentHashMap<>();
    endPoint.setBoundProtocol(this);
  }

  public abstract Subject getSubject();

  public void completedConnection() {
    if (!completed) {
      completed = true;
      endPoint.completedConnection();
    }
  }

  @Override
  public void close() throws IOException {
    if (mbean != null) {
      mbean.close();
    }
    endPoint.close();
  }

  public ParserExecutor getParser(String resource){
    return parserLookup.get(resource);
  }

  public void connect(String sessionId, String username, String password) throws IOException {
  }

  public void subscribeRemote(
      @NonNull @NotNull String resource,
      @NonNull @NotNull String mappedResource,
      @Nullable ParserExecutor parser,
      @Nullable Transformer transformer,
      @Nullable StatisticsConfigDTO statistics
  ) throws IOException {

    topicNameMapping.put(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(mappedResource, transformer);
    }
    if(parser != null){
      parserLookup.put(resource, parser);
    }
    if(statistics != null) {
      resourceNameAnalyserMap.put(resource, statistics);
    }
  }

  public void subscribeLocal(
      @NonNull @NotNull String resource,
      @NonNull @NotNull String mappedResource,
      @Nullable String selector,
      @Nullable Transformer transformer,
      @Nullable NamespaceFilters namespaceFilters,
      @Nullable StatisticsConfigDTO statistics
  )
      throws IOException {
    this.setNamespaceFilters(namespaceFilters);
    topicNameMapping.put(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(mappedResource, transformer);
    }
    if(statistics != null) {
      resourceNameAnalyserMap.put(resource, statistics);
    }

  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }

  public void receivedMessage() {
    endPoint.getEndPointStatus().incrementReceivedMessages();
    EndPoint.totalReceived.increment();
  }

  public void sentMessage() {
    endPoint.getEndPointStatus().incrementSentMessages();
    EndPoint.totalSent.increment();
  }

  public void sendKeepAlive() {
    // by default, we don't do anything. A protocol that needs to do something can override this function
  }

  protected NamespaceFilter filterMessage(MessageEvent messageEvent) throws IOException {
    String destinationName = messageEvent.getDestinationName();
    NamespaceFilters filters = getNamespaceFilters();
    if(filters == null) {
      return null;
    }
    NamespaceFilter namespaceFilter = filters.findMatch(destinationName);
    if(namespaceFilter != null &&
      namespaceFilter.getExecutor() != null &&
      !namespaceFilter.getExecutor().evaluate(messageEvent.getMessage())) {
      if (messageEvent.getCompletionTask() != null) {
        messageEvent.getCompletionTask().run();
      }
      throw new IOException("Failed filter selection");
    }
    return namespaceFilter;
  }

  @Override
  public long getTimeOut() {
    return keepAlive;
  }

  public void setConnected(boolean connected) {
    if (this.connected != connected) {
      this.connected = connected;
      try {
        if (connected) {
          endPoint.getServer().handleNewEndPoint(endPoint);
        } else {
          endPoint.getServer().handleCloseEndPoint(endPoint);
        }
      } catch (IOException ioException) {
        endPoint.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_FAILED, ioException);
        try {
          endPoint.close();
        } catch (IOException e) {
          // we are closing due to an exception, we know we are in an exception state but, we just need to tidy up
        }
      }
    }
  }

  protected Message processTransformer(String normalisedName, Message message) {
    Transformer transformer = destinationTransformerMap.get(normalisedName);
    if (transformer != null) {
      MessageBuilder mb = new MessageBuilder(message);
      mb = mb.setDestinationTransformer(transformer);
      message = mb.build();
    }
    return message;
  }

  public Transformer destinationTransformationLookup(String name) {
    return destinationTransformerMap.get(name);
  }

  protected SubscriptionContextBuilder createSubscriptionContextBuilder(String resource, String selector, QualityOfService qos, int receiveMax) {
    ClientAcknowledgement ackManger = qos.getClientAcknowledgement();
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(resource, ackManger);
    builder.setAlias(resource);
    builder.setQos(qos);
    builder.setAllowOverlap(true);
    builder.setReceiveMaximum(receiveMax);
    if (selector != null && !selector.isEmpty()) {
      builder.setSelector(selector);
    }
    return builder;
  }

  public abstract ProtocolInformationDTO getInformation();

  protected void updateInformation(ProtocolInformationDTO dto){
    dto.setSessionId(getSessionId());
    dto.setTimeout(getTimeOut());
    dto.setKeepAlive(keepAlive);
    if(transformation != null){
      dto.setMessageTransformationName(transformation.getName());
    }
    else{
      dto.setMessageTransformationName("none");
    }
    Map<String, String> tansMap = new LinkedHashMap<>();
    if(destinationTransformerMap != null){
      for(Map.Entry<String, Transformer > entry : destinationTransformerMap.entrySet()){
        tansMap.put(entry.getKey(), entry.getValue().getName());
      }
    }
    dto.setDestinationTransformationMapping(tansMap);
    Map<String, String> parseMap = new LinkedHashMap<>();
    if(parserLookup != null){
      for(Map.Entry<String, ParserExecutor > entry : parserLookup.entrySet()){
        parseMap.put(entry.getKey(), entry.getValue().toString());
      }
    }
    dto.setSelectorMapping(parseMap);
  }


  protected String scanForName(String destinationName) {
    for (Map.Entry<String, String> entry : topicNameMapping.entrySet()) {
      int index = entry.getKey().indexOf("#");
      if (index > 0) {
        String sub = entry.getKey().substring(0, index);
        if (destinationName.startsWith(sub)) {
          destinationName = entry.getValue() + destinationName.substring(sub.length());
        }
      }
    }
    return destinationName;
  }


  public void sendFrame(ServerPacket request) throws IOException {
    Packet packet = new Packet(ByteBuffer.allocate(100));
    request.packFrame(packet);
    packet.flip();
    endPoint.sendPacket(packet);

  }
}
