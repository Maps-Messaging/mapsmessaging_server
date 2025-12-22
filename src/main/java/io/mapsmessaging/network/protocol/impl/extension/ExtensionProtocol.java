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

package io.mapsmessaging.network.protocol.impl.extension;

import com.sun.security.auth.UserPrincipal;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.extension.api.DestinationContext;
import io.mapsmessaging.network.protocol.impl.extension.api.ServerApi;
import io.mapsmessaging.network.protocol.impl.extension.api.SessionContext;
import io.mapsmessaging.network.protocol.transformation.internal.MetaRouteHandler;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExtensionProtocol extends Protocol implements MessageListener, ClientConnection {
  private final Map<String, String> nameMapping;
  private final Map<String, ParserExecutor> parsers;

  private Principal principal;
  private final ServerApi serverApi;
  protected final EndPointURL endPointURL;
  private final Extension extension;
  private SessionContext session;
  private String sessionId;

  public ExtensionProtocol(@NonNull @NotNull EndPoint endPoint, @NonNull @NotNull Extension extension) {
    super(endPoint, new ProtocolConfigDTO());
    endPointURL = new EndPointURL(endPoint.getConfig().getUrl());
    this.extension = extension;
    serverApi = new ServerApi();
    nameMapping = new ConcurrentHashMap<>();
    parsers = new ConcurrentHashMap<>();
    extension.setExtensionProtocol(this);
  }

  public void connect(String sessionId, String username, String password) throws IOException {
    principal = new UserPrincipal(username);
    session = serverApi.createSession(this, sessionId, username, password);
    this.sessionId = sessionId;
    extension.initializeExtension();
  }

  @Override
  public Subject getSubject() {
    if(session == null) {
      return null;
    }
    return session.getSubject();
  }

  @Override
  public void close() throws IOException {
    extension.close();
  }

  @Override
  public void sendMessage(@org.jetbrains.annotations.NotNull @NonNull MessageEvent messageEvent) {
    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent);
    if(parsedMessage == null) {
      return;
    }
    extension.outbound(parsedMessage.getDestinationName(), parsedMessage.getMessage());
    messageEvent.getCompletionTask().run();
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, String selector, @Nullable InterServerTransformation transformer, @org.jetbrains.annotations.Nullable NamespaceFilters namespaceFilters, StatisticsConfigDTO statistics) throws IOException {
    nameMapping.put(resource, mappedResource);
    extension.registerLocalLink(mappedResource);
    if(transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    session.subscribe(resource, selector);
  }

  @Override
  public void subscribeRemote(@NonNull @org.jetbrains.annotations.NotNull String resource, @NonNull @org.jetbrains.annotations.NotNull String mappedResource, @NonNull @NotNull QualityOfService qos, @Nullable ParserExecutor parser, @Nullable InterServerTransformation transformer, StatisticsConfigDTO statistics) throws IOException {
    nameMapping.put(resource, mappedResource);
    if(!extension.supportsRemoteFiltering() && parser != null){
      parsers.put(resource, parser);
    }
    extension.registerRemoteLink(resource, extension.supportsRemoteFiltering()? parser.toString(): null );
  }

  protected int saveMessage(@NonNull @NotNull String destinationName, @NotNull Message message) throws ExecutionException, InterruptedException, TimeoutException, IOException {
    String lookup = nameMapping.get(destinationName);
    if(lookup == null)lookup = destinationName;
    if(lookup != null) {
      DestinationContext destination = session.getDestination(lookup, DestinationType.TOPIC);
      if (destination != null) {
        ParserExecutor parser = parsers.get(destinationName);
        Map<String, String> meta = message.getMeta() != null ? message.getMeta() : new LinkedHashMap<>();
        String host = extension.getExtensionProtocol().endPointURL.getHost();
        String id = extension.getName();
        meta = MetaRouteHandler.updateRoute(host, id, meta, System.currentTimeMillis());
        MessageBuilder messageBuilder = new MessageBuilder(message);
        messageBuilder.setMeta(meta);
        return destination.writeEvent(messageBuilder.build(), parser);
      }
    }
    return 0;
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    ProtocolInformationDTO dto = new ProtocolInformationDTO();
    dto.setSessionId(sessionId);
    dto.setMessageTransformationName("");
    dto.setType(extension.getName());
    return dto;
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    return false;
  }

  @Override
  public String getName() {
    return "LocalLoop";
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public String getVersion() {
    return extension.getVersion();
  }

  @Override
  public Principal getPrincipal() {
    return principal;
  }

  @Override
  public String getAuthenticationConfig() {
    return endPoint.getAuthenticationConfig();
  }

  @Override
  public String getUniqueName() {
    return endPointURL.toString();
  }

  @Override
  public String getProtocolName() {
    return "extension";
  }

  @Override
  public String getRemoteIp() {
    return "loop";
  }

}
