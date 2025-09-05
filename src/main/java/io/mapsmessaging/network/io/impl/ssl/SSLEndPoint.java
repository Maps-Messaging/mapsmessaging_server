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

package io.mapsmessaging.network.io.impl.ssl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.tcp.TCPEndPoint;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.Principal;
import java.util.List;

public class SSLEndPoint extends TCPEndPoint {

  private final ByteBuffer encryptedOut;
  private final ByteBuffer encryptedIn;

  //
  // Required by the Handshake Manager
  SSLHandshakeManager handshakeManager;
  SSLEngine sslEngine;

  public SSLEndPoint(long id, SSLEngine engine, SocketChannel accepted, Selector select, EndPointConnectedCallback callback, EndPointServerStatus endPointServerStatus,
      List<String> jmxParent) throws IOException {
    super(id, accepted, select, endPointServerStatus, jmxParent);
    sslEngine = engine;
    logger.log(ServerLogMessages.SSL_CREATE_ENGINE);
    int sessionSize = sslEngine.getSession().getPacketBufferSize();
    logger.log(ServerLogMessages.SSL_ENCRYPTION_BUFFERS, sessionSize);
    encryptedOut = ByteBuffer.allocateDirect(sessionSize);
    encryptedIn = ByteBuffer.allocateDirect(sessionSize);
    init(engine, callback);
    sendBuffer(ByteBuffer.allocate(0)); // Kick off the SSL handshake
    select.register(accepted, SelectionKey.OP_READ, handshakeManager);
  }


  public SSLEndPoint(long id, SSLEngine engine, Socket socket, Selector select, String authConfig, EndPointServer server, EndPointManagerJMX managerMBean)
      throws IOException {
    super(id, socket, select, authConfig, server, managerMBean);
    sslEngine = engine;
    logger.log(ServerLogMessages.SSL_CREATE_ENGINE);

    int sessionSize = sslEngine.getSession().getPacketBufferSize();
    logger.log(ServerLogMessages.SSL_ENCRYPTION_BUFFERS, sessionSize);
    encryptedOut = ByteBuffer.allocateDirect(sessionSize);
    encryptedIn = ByteBuffer.allocateDirect(sessionSize);

    init(engine, null);
  }

  private void init(SSLEngine engine, EndPointConnectedCallback callback) throws SSLException {
    sslEngine = engine;
    sslEngine.setUseClientMode(callback != null);

    logger.log(ServerLogMessages.SSL_HANDSHAKE_START);
    handshakeManager = new SSLHandShakeManagerImpl(this, callback);

    logger.log(ServerLogMessages.SSL_HANDSHAKE_READY);
    sslEngine.beginHandshake();
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    handshakeManager.handleSSLHandshakeStatus();
    int original = packet.position();
    sendBuffer(packet.getRawBuffer());
    logger.log(ServerLogMessages.SSL_SENT, (packet.position() - original));
    return packet.position() - original;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    if (!handshakeManager.handleSSLHandshakeStatus()) {
      int len;
      if(handshakeManager.getHandshakeBufferIn().hasRemaining()){
        packet.put(new Packet(handshakeManager.getHandshakeBufferIn()));
        len = packet.position();
      }
      else {
        len = readBuffer(packet.getRawBuffer());
      }
      logger.log(ServerLogMessages.SSL_READ, len);
      return len;
    }
    return 0;
  }

  @Override
  protected int sendBuffer(ByteBuffer applicationOutBuffer) throws IOException {
    int len = 0;
    SSLEngineResult result;
    do {
      result = handleSSLEngineResult(sslEngine.wrap(applicationOutBuffer, encryptedOut));
      encryptedOut.flip();
      len += super.sendBuffer(encryptedOut);
      encryptedOut.clear(); // Will need to deal with limited writes
    } while (result.getStatus() == Status.BUFFER_OVERFLOW);
    logger.log(ServerLogMessages.SSL_SEND_ENCRYPTED, len);

    return len;
  }

  @Override
  protected int readBuffer(ByteBuffer applicationIn) throws IOException {
    int response = super.readBuffer(encryptedIn);
    logger.log(ServerLogMessages.SSL_READ_ENCRYPTED, response, encryptedIn.position(), encryptedIn.limit());
    if (response > 0 || encryptedIn.position() != 0) {
      if (encryptedIn.limit() == encryptedIn.capacity()) {
        encryptedIn.flip();
        response = encryptedIn.limit();
      }
      logger.log(ServerLogMessages.SSL_READ_ENCRYPTED, response, encryptedIn.position(), encryptedIn.limit());
      do {
        SSLEngineResult result = sslEngine.unwrap(encryptedIn, applicationIn);
        handleSSLEngineResult(result);

        if (result.getStatus() == Status.BUFFER_UNDERFLOW) {
          encryptedIn.compact();
          return response;
        }
      } while (encryptedIn.hasRemaining() && applicationIn.remaining() != 0);

      if (encryptedIn.position() == encryptedIn.limit()) {
        encryptedIn.clear();
      } else {
        encryptedIn.compact();
      }
    }
    return response;
  }

  private SSLEngineResult handleSSLEngineResult(SSLEngineResult result) throws IOException {
    if (result.getStatus() == Status.CLOSED) {
      throw new IOException("Session closed");
    } else {
      logger.log(ServerLogMessages.SSL_ENGINE_RESULT, result.getStatus());
    }
    return result;
  }

  @Override
  public Principal getEndPointPrincipal() {
    if (sslEngine.getNeedClientAuth()) {
      try {
        return sslEngine.getSession().getPeerPrincipal();
      } catch (SSLPeerUnverifiedException e) {
        logger.log(ServerLogMessages.SSL_ENGINE_CLIENT_AUTH);
      }
    }
    return null;
  }

  @Override
  public String getProtocol() {
    return "ssl";
  }

  @Override
  public boolean isSSL() {
    return true;
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(SSLEndPoint.class.getName() + "_" + getId());
  }

}
