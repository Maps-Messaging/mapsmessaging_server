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

package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.BufferedReadEndPoint;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.FutureTask;

public class WebSocketEndPoint extends EndPoint implements BufferedReadEndPoint {

  private static final int NETWORK_READ_BUFFER_SIZE = 128 * 1024;

  private final EndPoint endPoint;
  private final Packet networkReadPacket;
  private final WebSocketFrameDecoder decoder;
  private final WebSocketFrameWriter writer;
  private final WebSocketFrameDecoder.Listener controlFrameListener;

  private boolean closeResponseSent;

  public WebSocketEndPoint(EndPoint endPoint) {
    this(endPoint, null);
  }

  public WebSocketEndPoint(EndPoint endPoint, Packet initialData) {
    super(endPoint.getId(), endPoint.getServer());
    this.endPoint = endPoint;
    String tmp = endPoint.getName();
    if (tmp.startsWith("tcp")) {
      name = "ws" + tmp.substring(3);
    } else if (tmp.startsWith("ssl")) {
      name = "wss" + tmp.substring(3);
    } else {
      name = tmp;
    }
    int initialBytes = initialData == null ? 0 : initialData.available();
    networkReadPacket = new Packet(Math.max(NETWORK_READ_BUFFER_SIZE, initialBytes), false);
    if (initialBytes > 0) {
      networkReadPacket.put(initialData);
    }
    decoder = new WebSocketFrameDecoder();
    writer = new WebSocketFrameWriter(endPoint::sendPacket);
    controlFrameListener = new ControlFrameListener();
  }

  @Override
  public void close() throws IOException {
    endPoint.close();
    super.close();
  }

  @Override
  public String getProtocol() {
    return endPoint.isSSL() ? "wss" : "ws";
  }

  @Override
  public boolean isUDP() {
    return false;
  }

  @Override
  public List<String> getJMXTypePath() {
    return endPoint.getJMXTypePath();
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    int consumed = writer.writeBinary(packet);
    int networkBytes = writer.getLastNetworkBytesWritten();
    if (networkBytes > 0) {
      updateWriteBytes(networkBytes);
    }
    return consumed;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int decoded = decoder.drain(packet);
    if (decoder.isCloseReceived()) {
      return -1;
    }
    if (!packet.hasRemaining()) {
      return packet.position();
    }

    decoded += decodeBuffered(packet);
    if (decoder.isCloseReceived()) {
      return -1;
    }
    if (decoded > 0 || !packet.hasRemaining()) {
      return packet.position();
    }

    int read = endPoint.readPacket(networkReadPacket);
    if (read <= 0) {
      return read;
    }
    updateReadBytes(read);

    decoded = decodeBuffered(packet);
    if (decoder.isCloseReceived()) {
      return -1;
    }
    return decoded > 0 ? packet.position() : 0;
  }

  @Override
  public boolean hasBufferedReadData() {
    return decoder.hasPendingOutput();
  }

  private int decodeBuffered(Packet destination) throws IOException {
    if (networkReadPacket.position() == 0) {
      return 0;
    }

    networkReadPacket.flip();
    int decoded;
    try {
      decoded = decoder.decode(networkReadPacket, destination, controlFrameListener);
    } catch (WebSocketProtocolException protocolError) {
      sendProtocolClose(protocolError.getCloseCode());
      throw protocolError;
    } finally {
      networkReadPacket.compact();
    }
    return decoded;
  }

  private void sendProtocolClose(int closeCode) {
    if (closeResponseSent) {
      return;
    }
    closeResponseSent = true;
    byte[] payload = {(byte) ((closeCode >>> 8) & 0xFF), (byte) (closeCode & 0xFF)};
    try {
      writer.writeControl(WebSocketFrameDecoder.CLOSE, payload);
      int networkBytes = writer.getLastNetworkBytesWritten();
      if (networkBytes > 0) {
        updateWriteBytes(networkBytes);
      }
    } catch (IOException ignored) {
      // The connection is closed by the caller after the protocol error.
    }
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return endPoint.register(selectionKey, runner);
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return endPoint.deregister(selectionKey);
  }

  @Override
  public String getAuthenticationConfig() {
    return endPoint.getAuthenticationConfig();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(WebSocketEndPoint.class);
  }

  @Override
  public String getRemoteSocketAddress() {
    return endPoint.getRemoteSocketAddress();
  }

  private final class ControlFrameListener implements WebSocketFrameDecoder.Listener {

    @Override
    public void onPing(byte[] payload) throws IOException {
      boolean written = writer.writeControl(WebSocketFrameDecoder.PONG, payload);
      int networkBytes = writer.getLastNetworkBytesWritten();
      if (networkBytes > 0) {
        updateWriteBytes(networkBytes);
      }
      if (!written && !writer.hasPendingApplicationData()) {
        throw new IOException("Unable to write WebSocket PONG frame");
      }
    }

    @Override
    public void onPong(byte[] payload) {
      // A PONG is an acknowledgement and does not require a response.
    }

    @Override
    public void onClose(byte[] payload) throws IOException {
      if (closeResponseSent) {
        return;
      }
      closeResponseSent = true;
      boolean written = writer.writeControl(WebSocketFrameDecoder.CLOSE, payload);
      int networkBytes = writer.getLastNetworkBytesWritten();
      if (networkBytes > 0) {
        updateWriteBytes(networkBytes);
      }
      if (!written) {
        throw new IOException("Unable to write WebSocket CLOSE response");
      }
    }
  }
}
