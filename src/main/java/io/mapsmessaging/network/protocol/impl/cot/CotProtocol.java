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

package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.security.ssl.SslHelper;
import io.mapsmessaging.state.drone.tak.TakSocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Accepts a raw, unframed stream of Cursor-on-Target (CoT) XML &lt;event&gt; documents (the wire
 * format used by TAK clients/servers) and republishes each complete document, byte for byte and
 * with no parsing, onto a fixed topic, and - when takHostname is configured - forwards it directly
 * over its own TakSocketConnection to a real TAK server, the same mechanism TwinManager's own
 * drone-state pipeline uses. There is no application level login on this connection - the session
 * authenticates as the built in anonymous identity, the same identity the existing anonymous Stomp
 * interface uses. Transport security is provided by binding this protocol to an ssl:// listener in
 * NetworkManager.yaml.
 *
 * <p>Discovered via the standard ProtocolImplFactory ServiceLoader registration. Logging uses
 * SLF4J for now (this protocol carries no {@code ServerLogMessages} constants of its own);
 * {@link #mapsLogger} exists because {@link SslHelper#createContext} takes a MAPS logger.
 */
public class CotProtocol extends Protocol implements Selectable {

  private static final String DESTINATION_NAME = "/tak/cot/inbound";
  private static final int MAX_BUFFER_SIZE = 1_048_576; // 1MB safety cap against a malformed/never-terminated stream

  private static final byte[] EVENT_START = "<event".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] EVENT_END = "</event>".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] XML_DECL_START = "<?xml".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] XML_DECL_END = "?>".getBytes(StandardCharsets.US_ASCII);

  private static final ExecutorService READER_POOL = Executors.newCachedThreadPool(runnable -> {
    Thread thread = new Thread(runnable, "cot-protocol-reader");
    thread.setDaemon(true);
    return thread;
  });

  private final Logger logger = LoggerFactory.getLogger(CotProtocol.class);
  private final io.mapsmessaging.logging.Logger mapsLogger =
      io.mapsmessaging.logging.LoggerFactory.getLogger(CotProtocol.class);
  private final Packet packet;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final String sessionId;
  private final TakSocketConnection takSocketConnection;

  private Session session;
  private boolean closed;

  public CotProtocol(EndPoint endPoint, Packet initialPacket, CotProtocolConfigDTO config) throws IOException {
    super(endPoint, config != null ? config : new CotProtocolConfigDTO());
    this.packet = initialPacket;
    this.sessionId = "cot-" + UUID.randomUUID();
    this.takSocketConnection = buildTakSocketConnection(config);
    createSession();
    logger.debug("CoT passthrough connection created on {}", endPoint.getConfig().getUrl());
    if (initialPacket.available() > 0) {
      appendAndProcess(initialPacket);
    }
    endPoint.register(SelectionKey.OP_READ, this);
  }

  private TakSocketConnection buildTakSocketConnection(CotProtocolConfigDTO config) throws IOException {
    if (config == null || config.getTakHostname() == null || config.getTakHostname().isBlank()) {
      return null;
    }
    if (!config.isTakTlsEnabled()) {
      return new TakSocketConnection(config.getTakHostname(), config.getTakPort());
    }
    ConfigurationProperties sslProps = new ConfigurationProperties();
    sslProps.put("keyStore", ((Config) config.getTakKeyStore()).toConfigurationProperties());
    sslProps.put("trustStore", ((Config) config.getTakTrustStore()).toConfigurationProperties());
    SSLContext sslContext = SslHelper.createContext(config.getTakTlsContext(), sslProps, mapsLogger);
    return new TakSocketConnection(config.getTakHostname(), config.getTakPort(), sslContext.getSocketFactory());
  }

  private void createSession() throws IOException {
    try {
      SessionContextBuilder scb = new SessionContextBuilder(sessionId, new ProtocolClientConnection(this));
      scb.setUsername("anonymous")
          .setPassword("".toCharArray())
          .setPersistentSession(false)
          .setSessionExpiry(0)
          .setReceiveMaximum(1);
      session = SessionManager.getInstance().create(scb.build(), this);
    } catch (LoginException e) {
      logger.error("CoT passthrough session creation failed", e);
      throw new IOException("Unable to create CoT passthrough session", e);
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    try {
      endPoint.deregister(SelectionKey.OP_READ);
    } catch (ClosedChannelException e) {
      logger.warn("Exception reading from CoT passthrough connection", e);
      return;
    }
    READER_POOL.execute(new ReadTask());
  }

  private final class ReadTask implements Runnable {
    @Override
    public void run() {
      Thread.currentThread().setName("CoT Protocol Reader::" + Thread.currentThread().getName());
      try {
        int read;
        boolean first = true;
        do {
          packet.clear();
          read = endPoint.readPacket(packet);
          if (read > 0) {
            EndPoint.totalReceived.increment();
            packet.flip();
            appendAndProcess(packet);
          } else if (first) {
            closeQuietly();
            return;
          }
          first = false;
        } while (read > 0);
        endPoint.register(SelectionKey.OP_READ, CotProtocol.this);
      } catch (Exception e) {
        logger.warn("Exception reading from CoT passthrough connection", e);
        closeQuietly();
      }
    }
  }

  private void appendAndProcess(Packet packet) throws IOException {
    byte[] chunk = new byte[packet.available()];
    packet.get(chunk);
    buffer.write(chunk);
    processBuffer();
  }

  private void processBuffer() {
    byte[] data = buffer.toByteArray();
    int scanFrom = 0;
    int consumedUpTo = 0;
    while (true) {
      int eventStart = indexOf(data, EVENT_START, scanFrom);
      if (eventStart < 0) {
        break;
      }
      int endTagIdx = indexOf(data, EVENT_END, eventStart + EVENT_START.length);
      if (endTagIdx < 0) {
        break; // Incomplete document, wait for more data
      }
      int docEnd = endTagIdx + EVENT_END.length;
      int docStart = findPrecedingXmlDeclaration(data, eventStart, consumedUpTo);
      byte[] document = Arrays.copyOfRange(data, docStart, docEnd);
      publish(document);
      consumedUpTo = docEnd;
      scanFrom = docEnd;
    }

    buffer.reset();
    if (consumedUpTo < data.length) {
      buffer.write(data, consumedUpTo, data.length - consumedUpTo);
    }
    if (buffer.size() > MAX_BUFFER_SIZE) {
      logger.warn("CoT passthrough buffer exceeded {} bytes without a complete event, discarding buffered data", MAX_BUFFER_SIZE);
      buffer.reset();
    }
  }

  private int findPrecedingXmlDeclaration(byte[] data, int eventStart, int lowerBound) {
    int declStart = lastIndexOf(data, XML_DECL_START, lowerBound, eventStart);
    if (declStart < 0) {
      return eventStart;
    }
    int declEnd = indexOf(data, XML_DECL_END, declStart);
    if (declEnd < 0 || declEnd + XML_DECL_END.length > eventStart) {
      return eventStart;
    }
    int afterDecl = declEnd + XML_DECL_END.length;
    if (!isWhitespaceOnly(data, afterDecl, eventStart)) {
      return eventStart;
    }
    return declStart;
  }

  private void publish(byte[] xml) {
    Message message = new MessageBuilder()
        .setOpaqueData(xml)
        .setContentType("text/xml")
        .setQoS(QualityOfService.AT_MOST_ONCE)
        .setRetain(false)
        .build();

    session.findDestination(DESTINATION_NAME, DestinationType.TOPIC).whenComplete((destination, throwable) -> {
      if (throwable != null) {
        logger.error("Failed to publish CoT event to {}", DESTINATION_NAME, throwable);
        return;
      }
      if (destination != null) {
        try {
          destination.storeMessage(message);
        } catch (IOException e) {
          logger.error("Failed to publish CoT event to {}", DESTINATION_NAME, e);
        }
      }
    });

    if (takSocketConnection != null) {
      takSocketConnection.accept(new String(xml, StandardCharsets.UTF_8));
    }
  }

  private static int indexOf(byte[] data, byte[] pattern, int from) {
    int max = data.length - pattern.length;
    outer:
    for (int i = Math.max(from, 0); i <= max; i++) {
      for (int j = 0; j < pattern.length; j++) {
        if (data[i + j] != pattern[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  private static int lastIndexOf(byte[] data, byte[] pattern, int from, int to) {
    int max = Math.min(to, data.length) - pattern.length;
    outer:
    for (int i = max; i >= from; i--) {
      for (int j = 0; j < pattern.length; j++) {
        if (data[i + j] != pattern[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  private static boolean isWhitespaceOnly(byte[] data, int from, int to) {
    for (int i = from; i < to; i++) {
      if (!Character.isWhitespace(data[i] & 0xff)) {
        return false;
      }
    }
    return true;
  }

  private void closeQuietly() {
    if (closed) {
      return;
    }
    closed = true;
    if (takSocketConnection != null) {
      takSocketConnection.close();
    }
    try {
      if (session != null) {
        SessionManager.getInstance().close(session, false);
      }
    } catch (IOException e) {
      logger.warn("Exception reading from CoT passthrough connection", e);
    }
    try {
      super.close();
    } catch (IOException e) {
      logger.warn("Exception reading from CoT passthrough connection", e);
    }
    logger.debug("CoT passthrough connection closed");
  }

  @Override
  public void close() throws IOException {
    closeQuietly();
  }

  @Override
  public Subject getSubject() {
    return session != null ? session.getSecurityContext().getSubject() : null;
  }

  @Override
  public void sendMessage(MessageEvent messageEvent) {
    // This is an inbound only listener, nothing is ever subscribed through it
    if (messageEvent.getCompletionTask() != null) {
      messageEvent.getCompletionTask().run();
    }
  }

  @Override
  public void sendKeepAlive() {
    // No keep alive required for this passthrough listener
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    return false; // Reading is handled directly via the selector callback + ReadTask
  }

  @Override
  public String getName() {
    return "CoT";
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    ProtocolInformationDTO dto = new ProtocolInformationDTO();
    updateInformation(dto);
    return dto;
  }
}
