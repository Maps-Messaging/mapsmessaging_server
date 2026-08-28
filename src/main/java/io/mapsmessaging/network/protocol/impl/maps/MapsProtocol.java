/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MapsConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.MapsProtocolInformation;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.maps.listeners.MapsPacketListener;
import io.mapsmessaging.network.protocol.impl.maps.listeners.MapsPacketListenerFactory;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapsProtocol extends Protocol {

  public static final int STATUS_OK = 0;
  public static final int STATUS_UNSUPPORTED_VERSION = 1;
  public static final int STATUS_AUTH_FAILED = 2;
  public static final int STATUS_ERROR = 3;

  private final MapsConfigDTO mapsConfig;
  private final SelectorTask selectorTask;
  private final MapsFrameDecoder decoder;
  private final MapsPacketListenerFactory listenerFactory;
  private final AtomicInteger requestIds = new AtomicInteger(1);
  private final Map<Integer, Runnable> pendingAcks = new ConcurrentHashMap<>();

  private volatile Session session;
  private volatile boolean closed;
  private volatile int negotiatedMajor = 1;
  private volatile int negotiatedMinor = 0;
  private volatile long remoteCapabilities;
  private String outboundSessionId;
  private String outboundUsername;
  private String outboundPassword;

  public MapsProtocol(EndPoint endPoint) throws IOException {
    super(endPoint, resolveConfig(endPoint));
    mapsConfig = (MapsConfigDTO) protocolConfig;
    keepAlive = mapsConfig.getKeepAlive() * 1000L;
    decoder = new MapsFrameDecoder(mapsConfig.getMaximumFrameSize());
    listenerFactory = new MapsPacketListenerFactory();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
  }

  public MapsProtocol(EndPoint endPoint, Packet initialPacket) throws IOException {
    this(endPoint);
    processPacket(initialPacket);
    selectorTask.getReadTask().pushOutstandingData(initialPacket);
  }

  private static ProtocolConfigDTO resolveConfig(EndPoint endPoint) {
    ProtocolConfigDTO config = endPoint.getConfig().getProtocolConfig("maps");
    return config instanceof MapsConfigDTO ? config : new MapsConfigDTO();
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    outboundSessionId = sessionId;
    outboundUsername = username;
    outboundPassword = password;
    int size = 4 + Long.BYTES + Integer.BYTES + MapsCodec.stringSize(sessionId) + 1 + MapsCodec.stringSize(username) + MapsCodec.stringSize(password);
    ByteBuffer body = ByteBuffer.allocate(size);
    body.put((byte) 1).put((byte) 0).put((byte) 1).put((byte) 0);
    body.putLong(MapsCapabilities.VERSION_1);
    body.putInt(mapsConfig.getKeepAlive());
    MapsCodec.putString(body, sessionId);
    body.put((byte) (username == null ? 0 : 1));
    MapsCodec.putString(body, username);
    MapsCodec.putString(body, password);
    body.flip();
    writeFrame(new MapsFrame(MapsPacketType.CONNECT, 0, nextRequestId(), body));
    registerRead();
    completedConnection();
  }

  public void handleConnect(MapsFrame frame) throws IOException {
    ByteBuffer body = frame.body();
    MapsCodec.require(body, 4 + Long.BYTES + Integer.BYTES);
    int minMajor = Byte.toUnsignedInt(body.get());
    int minMinor = Byte.toUnsignedInt(body.get());
    int maxMajor = Byte.toUnsignedInt(body.get());
    int maxMinor = Byte.toUnsignedInt(body.get());
    long capabilities = body.getLong();
    int requestedKeepAlive = body.getInt();
    String sessionId = MapsCodec.getString(body);
    MapsCodec.require(body, 1);
    int authType = Byte.toUnsignedInt(body.get());
    String username = MapsCodec.getString(body);
    String password = MapsCodec.getString(body);

    if (minMajor > 1 || maxMajor < 1) {
      sendConnAck(frame.requestId(), STATUS_UNSUPPORTED_VERSION, "MAPS/1 is not supported by peer version range");
      return;
    }

    negotiatedMajor = 1;
    negotiatedMinor = Math.min(maxMinor, 0);
    remoteCapabilities = capabilities;
    keepAlive = Math.max(1000L, requestedKeepAlive * 1000L);

    try {
      SessionContextBuilder builder = new SessionContextBuilder(sessionId, new ProtocolClientConnection(this));
      builder.setResetState(true).setPersistentSession(false).setReceiveMaximum(mapsConfig.getReceiveMaximum());
      if (authType == 1) {
        builder.setUsername(username);
        if (password != null) builder.setPassword(password.toCharArray());
      }
      Session created = SessionManager.getInstance().create(builder.build(), this);
      created.login();
      session = created;
      sendConnAck(frame.requestId(), STATUS_OK, "connected");
      session.resumeState();
      setConnected(true);
    } catch (LoginException | IOException exception) {
      sendConnAck(frame.requestId(), STATUS_AUTH_FAILED, "authentication failed");
    }
  }

  public void handleConnAck(MapsFrame frame) throws IOException {
    ByteBuffer body = frame.body();
    MapsCodec.require(body, 3 + Long.BYTES);
    int status = Byte.toUnsignedInt(body.get());
    negotiatedMajor = Byte.toUnsignedInt(body.get());
    negotiatedMinor = Byte.toUnsignedInt(body.get());
    remoteCapabilities = body.getLong();
    MapsCodec.getString(body);
    String detail = MapsCodec.getString(body);
    if (status != STATUS_OK) {
      throw new IOException("MAPS connection rejected: " + detail);
    }
    try {
      SessionContextBuilder builder = new SessionContextBuilder(outboundSessionId, new ProtocolClientConnection(this));
      builder.setResetState(true).setPersistentSession(false).setReceiveMaximum(mapsConfig.getReceiveMaximum());
      if (outboundUsername != null) {
        builder.setUsername(outboundUsername);
        if (outboundPassword != null) builder.setPassword(outboundPassword.toCharArray());
      }
      session = SessionManager.getInstance().create(builder.build(), this);
      session.login();
      session.resumeState();
      setConnected(true);
    } catch (LoginException exception) {
      throw new IOException("Unable to establish local MAPS session", exception);
    }
  }

  private void sendConnAck(int requestId, int status, String detail) {
    String serverId = MessageDaemon.getInstance() == null ? "" : MessageDaemon.getInstance().getId();
    ByteBuffer body = ByteBuffer.allocate(3 + Long.BYTES + MapsCodec.stringSize(serverId) + MapsCodec.stringSize(detail));
    body.put((byte) status).put((byte) negotiatedMajor).put((byte) negotiatedMinor);
    body.putLong(MapsCapabilities.VERSION_1);
    MapsCodec.putString(body, serverId);
    MapsCodec.putString(body, detail);
    body.flip();
    writeFrame(new MapsFrame(MapsPacketType.CONNACK, 0, requestId, body));
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos,
      @Nullable ParserExecutor parser, @Nullable InterServerTransformation transformer, StatisticsConfigDTO statistics, Map<String, Object> linkProperties) throws IOException {
    super.subscribeRemote(resource, mappedResource, qos, parser, transformer, statistics, linkProperties);
    String selector = parser == null ? null : parser.toString();
    ByteBuffer body = ByteBuffer.allocate(MapsCodec.stringSize(resource) + MapsCodec.stringSize(selector) + 1 + Integer.BYTES);
    MapsCodec.putString(body, resource);
    MapsCodec.putString(body, selector);
    body.put((byte) qos.getLevel());
    body.putInt(mapsConfig.getReceiveMaximum());
    body.flip();
    writeFrame(new MapsFrame(MapsPacketType.SUBSCRIBE, 0, nextRequestId(), body));
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @NonNull @NotNull QualityOfService qos,
      @Nullable String selector, @Nullable InterServerTransformation transformer, @Nullable NamespaceFilters namespaceFilters,
      StatisticsConfigDTO statistics, Map<String, Object> linkProperties) throws IOException {
    super.subscribeLocal(resource, mappedResource, qos, selector, transformer, namespaceFilters, statistics, linkProperties);
    ensureSession();
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(resource, selector, qos, mapsConfig.getReceiveMaximum());
    session.addSubscription(builder.build());
    session.resumeState();
  }

  @Override
  public void unsubscribeRemote(String resource) {
    ByteBuffer body = ByteBuffer.allocate(MapsCodec.stringSize(resource));
    MapsCodec.putString(body, resource);
    body.flip();
    writeFrame(new MapsFrame(MapsPacketType.UNSUBSCRIBE, 0, nextRequestId(), body));
  }

  @Override
  public void unsubscribeLocal(String resource) {
    if (session != null) session.removeSubscription(resource);
  }

  public void handleSubscribe(MapsFrame frame) throws IOException {
    ensureSession();
    ByteBuffer body = frame.body();
    String topic = MapsCodec.getString(body);
    String selector = MapsCodec.getString(body);
    MapsCodec.require(body, 1 + Integer.BYTES);
    QualityOfService qos = QualityOfService.getInstance(Byte.toUnsignedInt(body.get()));
    int receiveMaximum = body.getInt();
    SubscriptionContextBuilder builder = createSubscriptionContextBuilder(topic, selector, qos, receiveMaximum);
    session.addSubscription(builder.build());
    session.resumeState();
    sendAck(MapsPacketType.SUBACK, frame.requestId(), STATUS_OK, null);
  }

  public void handleUnsubscribe(MapsFrame frame) throws IOException {
    ensureSession();
    String topic = MapsCodec.getString(frame.body());
    session.removeSubscription(topic);
    sendAck(MapsPacketType.UNSUBACK, frame.requestId(), STATUS_OK, null);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    ParsedMessage parsed = parseOutboundMessage(messageEvent);
    if (parsed == null) return;
    QualityOfService qos = messageEvent.getSubscription().getContext().getQualityOfService();
    boolean ackRequired = !QualityOfService.AT_MOST_ONCE.equals(qos);
    int requestId = ackRequired ? nextRequestId() : 0;
    try {
      MapsPublishFrame frame = new MapsPublishFrame(parsed.getDestinationName(), parsed.getMessage(), requestId, ackRequired);
      Runnable completion = messageEvent.getCompletionTask();
      if (ackRequired) {
        if (completion != null) pendingAcks.put(requestId, completion);
      } else if (completion != null) {
        frame.onComplete(completion);
      }
      writeFrame(frame);
    } catch (IOException exception) {
      if (messageEvent.getCompletionTask() != null) messageEvent.getCompletionTask().run();
      try { close(); } catch (IOException ignored) { }
    }
  }

  public void handlePublish(MapsFrame frame) throws IOException {
    ensureSession();
    ByteBuffer body = frame.body();
    String topic = MapsCodec.getString(body);
    MapsCodec.require(body, Integer.BYTES);
    int count = body.getInt();
    if (count < 2 || count > 32) throw new IOException("Invalid MAPS Message buffer count " + count);
    MapsCodec.require(body, count * Integer.BYTES);
    int[] lengths = new int[count];
    long total = 0;
    for (int i = 0; i < count; i++) {
      lengths[i] = body.getInt();
      if (lengths[i] < 0) throw new IOException("Invalid MAPS Message buffer length");
      total += lengths[i];
    }
    if (total > body.remaining()) throw new IOException("Truncated MAPS native Message");
    ByteBuffer[] packed = new ByteBuffer[count];
    for (int i = 0; i < count; i++) {
      ByteBuffer slice = body.slice();
      slice.limit(lengths[i]);
      packed[i] = slice;
      body.position(body.position() + lengths[i]);
    }
    Message message = MessageFactory.getInstance().unpack(packed);
    ParsedMessage parsed = parseInboundMessage(parseForLookup(topic), message);
    if (parsed == null) {
      if (frame.ackRequired()) sendAck(MapsPacketType.PUBACK, frame.requestId(), STATUS_OK, null);
      return;
    }
    String destinationName = parsed.getDestinationName();
    Message inbound = parsed.getMessage();
    session.findDestination(destinationName, DestinationType.TOPIC).whenComplete((destination, error) -> storeInbound(frame, destination, inbound, error));
  }

  private void storeInbound(MapsFrame frame, Destination destination, Message message, Throwable error) {
    if (error != null || destination == null) {
      if (frame.ackRequired()) sendAck(MapsPacketType.PUBACK, frame.requestId(), STATUS_ERROR, "destination unavailable");
      return;
    }
    try {
      destination.storeMessage(message);
      receivedMessage();
      if (frame.ackRequired()) sendAck(MapsPacketType.PUBACK, frame.requestId(), STATUS_OK, null);
    } catch (IOException exception) {
      if (frame.ackRequired()) sendAck(MapsPacketType.PUBACK, frame.requestId(), STATUS_ERROR, exception.getMessage());
    }
  }

  public void handleAck(MapsFrame frame) throws IOException {
    ByteBuffer body = frame.body();
    MapsCodec.require(body, 1);
    int status = Byte.toUnsignedInt(body.get());
    String detail = MapsCodec.getString(body);
    Runnable callback = pendingAcks.remove(frame.requestId());
    if (callback != null) callback.run();
    if (status != STATUS_OK && frame.type() == MapsPacketType.PUBACK) throw new IOException("MAPS publish rejected: " + detail);
  }

  public void handlePing(MapsFrame frame) {
    writeFrame(new MapsFrame(MapsPacketType.PONG, 0, frame.requestId(), ByteBuffer.allocate(0)));
  }

  public void handlePong(MapsFrame frame) {
    // Receipt updates EndPoint last-read time; no additional state is required for MAPS/1.
  }

  public void handleDisconnect(MapsFrame frame) throws IOException {
    close();
  }

  public void sendAck(MapsPacketType type, int requestId, int status, String detail) {
    ByteBuffer body = ByteBuffer.allocate(1 + MapsCodec.stringSize(detail));
    body.put((byte) status);
    MapsCodec.putString(body, detail);
    body.flip();
    writeFrame(new MapsFrame(type, 0, requestId, body));
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    List<MapsFrame> frames = decoder.decode(packet.getRawBuffer());
    for (MapsFrame frame : frames) {
      if (frame.major() != 1) throw new IOException("Unsupported MAPS major protocol version " + frame.major());
      MapsPacketListener listener = listenerFactory.get(frame.type());
      if (listener == null) throw new IOException("No MAPS listener for " + frame.type());
      listener.handle(frame, this);
    }
    registerRead();
    return true;
  }

  public void registerRead() throws IOException {
    selectorTask.register(SelectionKey.OP_READ);
  }

  public void writeFrame(ServerPacket frame) {
    if (endPoint.isClosed()) return;
    sentMessage();
    selectorTask.push(frame);
  }

  private int nextRequestId() {
    int id = requestIds.getAndUpdate(value -> value == Integer.MAX_VALUE ? 1 : value + 1);
    return id == 0 ? requestIds.getAndIncrement() : id;
  }

  private void ensureSession() throws IOException {
    if (session == null || session.isClosed()) throw new IOException("MAPS session is not established");
  }

  @Override
  public void sendKeepAlive() {
    if (!closed && session != null && !session.isClosed()) writeFrame(new MapsFrame(MapsPacketType.PING, 0, nextRequestId(), ByteBuffer.allocate(0)));
  }

  @Override
  public Subject getSubject() {
    return session == null ? new Subject() : session.getSecurityContext().getSubject();
  }

  @Override
  public String getName() { return "MAPS"; }

  @Override
  public String getSessionId() { return session == null ? (outboundSessionId == null ? "waiting" : outboundSessionId) : session.getName(); }

  @Override
  public String getVersion() { return negotiatedMajor + "." + negotiatedMinor; }

  @Override
  public ProtocolInformationDTO getInformation() {
    MapsProtocolInformation information = new MapsProtocolInformation();
    updateInformation(information);
    information.setLocalCapabilities(MapsCapabilities.VERSION_1);
    information.setRemoteCapabilities(remoteCapabilities);
    information.setNegotiatedMajor(negotiatedMajor);
    information.setNegotiatedMinor(negotiatedMinor);
    return information;
  }

  @Override
  public void close() throws IOException {
    if (closed) return;
    closed = true;
    pendingAcks.clear();
    if (session != null && !session.isClosed()) SessionManager.getInstance().close(session, false);
    if (selectorTask.isOpen()) selectorTask.close();
    super.close();
  }
}
