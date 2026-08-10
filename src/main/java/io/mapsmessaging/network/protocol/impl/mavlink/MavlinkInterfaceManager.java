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

package io.mapsmessaging.network.protocol.impl.mavlink;

import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_FAILED_FORWARD_PACKET;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_FAILED_PARSING_FORWARD_LIST;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_FAILED_SETTING_UP_SESSION;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_SESSION_CREATED;
import static io.mapsmessaging.logging.ServerLogMessages.MAVLINK_SUCCESSFUL_FORWARD_PACKET;

import io.mapsmessaging.config.protocol.impl.MavlinkConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.MavlinkEventFactory;
import io.mapsmessaging.mavlink.tlog.MavlinkTlogWriter;
import io.mapsmessaging.mavlink.tlog.TlogConfiguration;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.udp.UDPFacadeEndPoint;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MavlinkInterfaceManager implements SelectorCallback, MavlinkConnectionManager {

  private static final Logger logger = LoggerFactory.getLogger(MavlinkInterfaceManager.class);

  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final MavLinkSessionManager<MavlinkProtocol> currentSessions;
  private final MavlinkConfig mavlinkConfig;
  private final List<InetSocketAddress> forwardList;
  private final MavlinkTlogWriter tlogWriter;

  public MavlinkInterfaceManager(EndPoint endPoint) throws IOException {
    this.endPoint = endPoint;
    mavlinkConfig = (MavlinkConfig) endPoint.getConfig().getProtocolConfig("mavlink");
    long timeout = mavlinkConfig.getIdleSessionTimeout();
    currentSessions = new MavLinkSessionManager<>(timeout);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);

    forwardList = new ArrayList<>();
    String urlList = mavlinkConfig.getForwardUrls();
    if (urlList != null && !urlList.isBlank()) {
      String[] urls = urlList.split(",");
      for (String remote : urls) {
        if (!remote.isBlank()) {
          try {
            URI uri = URI.create(remote);
            forwardList.add(new InetSocketAddress(uri.getHost(), uri.getPort()));
          } catch (Exception e) {
            logger.log(MAVLINK_FAILED_PARSING_FORWARD_LIST, remote, e);
          }
        }
      }
    }

    tlogWriter = createTlogWriter();
  }

  public static MavlinkEventFactory loadDialect(String name) throws IOException {
    if (name == null || name.isBlank()) {
      return new MavlinkEventFactory();
    }
    return new MavlinkEventFactory(name);
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    try {
      SocketAddress fromAddress = packet.getFromAddress();
      if (fromAddress == null) {
        return true;
      }

      byte[] raw = new byte[packet.available()];
      int pos = packet.position();
      packet.get(raw);
      packet.position(pos);
      boolean forwardedSource = fromForward(fromAddress);
      List<byte[]> packets = MavlinkFrameExtractor.extractMavlinkFrames(raw);
      workThroughPackets(packets, packet, fromAddress, forwardedSource);
      return true;
    } finally {
      selectorTask.register(SelectionKey.OP_READ);
    }
  }

  private void workThroughPackets(List<byte[]> packets, Packet packet, SocketAddress fromAddress, boolean forwardedSource) throws IOException {
    for(byte[] data:packets) {
      writeTlog(data);
      int systemId = MavlinkFrameExtractor.getSystemId(data);
      if (!isAllowedSystem(systemId)) {
        if (!forwardedSource) {
          forwardPacket(data);
        }
      }
      else {
        MavlinkDeviceKey key = buildKey(packet, systemId);
        UDPSessionState<MavlinkProtocol> state = findOrCreate(key);
        if (state != null && state.getContext() != null) {
          state.getContext().processRawFrame(data, fromAddress.toString());
          if (!forwardedSource) {
            forwardPacket(data);
          }
        }
      }
    }
  }

  private boolean isAllowedSystem(int systemId) {
    return mavlinkConfig.getAcceptedSources() == null
        || mavlinkConfig.getAcceptedSources().isEmpty()
        || mavlinkConfig.getAcceptedSources().stream().anyMatch(knownSource -> knownSource.getSystemId() == systemId);
  }

  private MavlinkTlogWriter createTlogWriter() throws IOException {
    String tlogDirectory = mavlinkConfig.getTlogDirectory();
    if (tlogDirectory == null || tlogDirectory.isBlank()) {
      return null;
    }

    String fileName = endPoint.getName();
    if (!fileName.toLowerCase(Locale.ROOT).endsWith(".tlog")) {
      fileName += ".tlog";
    }
    fileName = toSafeFileName(fileName);
    Path tlogFile = Path.of(tlogDirectory).resolve(fileName);
    return new MavlinkTlogWriter(TlogConfiguration.builder(tlogFile).build());
  }

  @Override
  public void writeTlog(byte[] frameBytes) {
    if (tlogWriter == null || frameBytes == null || frameBytes.length == 0) {
      return;
    }

    Instant now = Instant.now();
    long timestampMicros = now.getEpochSecond() * 1_000_000L + now.getNano() / 1_000L;
    tlogWriter.write(timestampMicros, frameBytes);
  }

  private MavlinkDeviceKey buildKey(Packet packet, int systemId) {
    return new MavlinkDeviceKey(0, (InetSocketAddress) packet.getFromAddress(), systemId);
  }

  private synchronized UDPSessionState<MavlinkProtocol> findOrCreate(MavlinkDeviceKey key) {
    UDPSessionState<MavlinkProtocol> state = currentSessions.getState(key);
    if (state != null) {
      return state;
    }

    UDPFacadeEndPoint facade = new UDPFacadeEndPoint(endPoint, key.getRemoteAddress(), endPoint.getServer());
    try {
      MavlinkProtocol protocol = new MavlinkProtocol(this, key, facade, this.mavlinkConfig);
      state = new UDPSessionState<>(protocol);
      currentSessions.addState(key, state);
      logger.log(MAVLINK_SESSION_CREATED, key.toString());
      return state;
    } catch (IOException | RuntimeException e) {
      try {
        facade.close();
      } catch (IOException closeException) {
        e.addSuppressed(closeException);
      }
      logger.log(MAVLINK_FAILED_SETTING_UP_SESSION, key.toString(), e);
      return null;
    }
  }

  private boolean fromForward(SocketAddress fromAddress) {
    return forwardList.stream().anyMatch(forwardAddress -> forwardAddress.equals(fromAddress));
  }

  private void forwardPacket(byte[] raw) {
    for (SocketAddress socketAddress : forwardList) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(raw);
      Packet forward = new Packet(byteBuffer);
      forward.setFromAddress(socketAddress);
      try {
        endPoint.sendPacket(forward);
        logger.log(MAVLINK_SUCCESSFUL_FORWARD_PACKET, socketAddress.toString());
      } catch (IOException e) {
        logger.log(MAVLINK_FAILED_FORWARD_PACKET, socketAddress.toString(), e);
      }
    }
  }

  @Override
  public void close() {
    try {
      currentSessions.close();
    } finally {
      if (tlogWriter != null) {
        tlogWriter.close();
      }
    }
  }

  @Override
  public String getName() {
    return "Mavlink";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }

  public void close(MavlinkDeviceKey remoteClient) {
    currentSessions.deleteState(remoteClient);
  }

  private static String toSafeFileName(String value) {
    if (value == null || value.isBlank()) {
      return "mavlink";
    }

    StringBuilder safe = new StringBuilder(value.length());
    boolean previousUnderscore = false;

    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);

      if (Character.isLetterOrDigit(character) || character == '-' || character == '.') {
        safe.append(character);
        previousUnderscore = false;
      } else if (!previousUnderscore) {
        safe.append('_');
        previousUnderscore = true;
      }
    }

    int start = 0;
    int end = safe.length();

    while (start < end && (safe.charAt(start) == '.' || safe.charAt(start) == '_')) {
      start++;
    }

    while (end > start && (safe.charAt(end - 1) == '.' || safe.charAt(end - 1) == '_')) {
      end--;
    }

    if (start == end) {
      return "mavlink";
    }

    return safe.substring(start, end);
  }
}
