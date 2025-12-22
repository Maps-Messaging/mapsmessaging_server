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

package io.mapsmessaging.network.io.impl.tcp;

import io.mapsmessaging.dto.rest.config.network.impl.TcpConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.*;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.logging.ServerLogMessages.*;
import static io.mapsmessaging.network.io.impl.tcp.NetworkHelper.isInCidr;

public class TCPEndPoint extends EndPoint {

  protected final Socket socket;
  protected final SocketChannel socketChannel;
  protected final Selector selector;
  private final String authenticationConfig;
  private final EndPointJMX mbean;
  private final AtomicBoolean isClosed;

  public TCPEndPoint(long id, SocketChannel accepted, Selector select, EndPointServerStatus endPointServerStatus, List<String> jmxParent) throws IOException {
    super(id, endPointServerStatus);
    try {
      logger.log(TCP_ACCEPT_START, accepted);
      isClosed = new AtomicBoolean(false);
      socket = accepted.socket();
      socketChannel = accepted;
      selector = select;
      authenticationConfig = null;
      if (isClient()) {
        name = getProtocol() + "_" + socket.getLocalAddress().toString() + "_" + socket.getLocalPort();
      } else {
        name = getProtocol() + "_" + getRemoteSocketAddress();
      }
      configure((TcpConfigDTO) endPointServerStatus.getConfig().getEndPointConfig());
    } catch (IOException e) {
      logger.log(ServerLogMessages.TCP_CONNECT_FAILED, e, accepted.toString());
      throw e;
    }
    if (jmxParent != null && !jmxParent.isEmpty()) {
      mbean = new EndPointJMX(jmxParent, this);
      jmxParentPath = mbean.getTypePath();
    } else {
      mbean = null;
      jmxParentPath = new ArrayList<>();
    }
  }

  public TCPEndPoint(long id, Socket accepted, Selector select, String authConfig, EndPointServer server, EndPointManagerJMX managerMBean) throws IOException {
    super(id, server);
    try {
      logger.log(TCP_ACCEPT_START, accepted);
      isClosed = new AtomicBoolean(false);
      socket = accepted;
      socketChannel = socket.getChannel();
      selector = select;
      authenticationConfig = authConfig;
      name = getProtocol() + "_" + getRemoteSocketAddress();
      configure((TcpConfigDTO) server.getConfig().getEndPointConfig());
    } catch (IOException e) {
      logger.log(ServerLogMessages.TCP_CONNECT_FAILED, e, accepted.toString());
      throw e;
    }
    if (managerMBean != null) {
      mbean = new EndPointJMX(managerMBean.getTypePath(), this);
      jmxParentPath = mbean.getTypePath();
    } else {
      mbean = null;
      jmxParentPath = new ArrayList<>();
    }
  }


  @Override
  public void close() {
    if (!isClosed.getAndSet(true)) {
      logger.log(ServerLogMessages.TCP_CONNECTION_CLOSE, name);
      try {
        super.close();
        deregister(-1);
        selector.wakeup();
        socket.shutdownInput();
        socket.shutdownOutput();
        socketChannel.close();
        socket.close();
        logger.log(ServerLogMessages.TCP_CLOSE_SUCCESS, name);
      } catch (IOException e) {
        logger.log(ServerLogMessages.TCP_CLOSE_EXCEPTION, e, name);
      } finally {
        if (mbean != null) mbean.close();
        if (server != null) {
          server.handleCloseEndPoint(this);
        }
      }
    }
  }

  public void setProxyProtocolInfo(ProxyProtocolInfo proxyProtocolInfo){
    super.setProxyProtocolInfo(proxyProtocolInfo);
    name = getProtocol() + "_" + getRemoteSocketAddress();
  }

  public boolean isValidProxySource() {
    if (socket == null || proxyProtocolInfo == null) return false;
    InetAddress remoteAddr = socket.getInetAddress();
    if (remoteAddr == null) return false;
    String actualRemote = remoteAddr.getHostAddress();
    String proxySource = proxyProtocolInfo.getDestination().getAddress().getHostAddress();
    return(actualRemote.equals(proxySource));
  }

  public boolean isProxyAllowed() {
    String allowedProxyHosts = getConfig().getEndPointConfig().getAllowedProxyHosts();
    if (!isValidProxySource()) return false;
    if (allowedProxyHosts == null || allowedProxyHosts.isBlank()) return true;

    String sourceHost = proxyProtocolInfo.getDestination().getAddress().getHostAddress();
    String[] entries = allowedProxyHosts.split(",");

    for (String entry : entries) {
      String trimmed = entry.trim();
      if (trimmed.isEmpty()) continue;

      try {
        if (trimmed.contains("/")) {
          if (isInCidr(trimmed, sourceHost)) return true;
        } else {
          InetAddress allowed = InetAddress.getByName(trimmed);
          if (allowed.getHostAddress().equals(sourceHost)) return true;
        }
      } catch (Exception ignored) {
      }
    }
    return false;
  }

  public String getRemoteSocketAddress() {
    if (getProxyProtocolInfo() == null) {
      return socket.getRemoteSocketAddress().toString();
    }
    return getProxyProtocolInfo().getSource().getAddress().getHostAddress();
  }


  public String getProtocol() {
    return "tcp";
  }

  @Override
  public FutureTask<SelectionKey> register(int selection, Selectable runner) {
    return selector.register(socketChannel, selection, runner);
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selection) {
    return selector.register(socketChannel, 0, null);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getAuthenticationConfig() {
    return authenticationConfig;
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    int sent = sendBuffer(packet.getRawBuffer());
    logger.log(TCP_SEND_BUFFER, sent);
    return sent;
  }

  protected int sendBuffer(ByteBuffer bb) throws IOException {
    if (!isClosed.get()) {
      int count = socketChannel.write(bb);
      updateWriteBytes(count);
      return count;
    } else {
      throw new IOException("Socket has been closed");
    }
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int read = readBuffer(packet.getRawBuffer());
    logger.log(TCP_READ_BUFFER, read);
    return read;
  }

  protected int readBuffer(ByteBuffer bb) throws IOException {
    if (!socketChannel.isConnected() || isClosed.get()) {
      this.server.incrementError();
      throw new IOException("Socket closed");
    }
    int count = socketChannel.read(bb);
    if (count < 0) {
      this.server.incrementError();
      throw new IOException("Socket closed");
    }
    if(count > 0) {
      updateReadBytes(count);
    }
    return count;
  }

  @Override
  public String toString() {
    return socketChannel.socket().toString();
  }

  protected Logger createLogger() {
    return LoggerFactory.getLogger(TCPEndPoint.class.getName() + "_" + getId());
  }

  private void configure(TcpConfigDTO config) throws IOException {
    /*
    These are NOT configurable and required for normal operation
     */
    socket.setKeepAlive(true);
    socket.setReuseAddress(true);
    socket.setTcpNoDelay(true);
    socket.setSoLinger(true, 0);

    /*
    Configurable settings for Sockets
     */
    int receiveSize = config.getReceiveBufferSize();
    int sendSize = config.getSendBufferSize();
    int socketTimeOut = config.getTimeout();

    logger.log(TCP_CONFIGURED_PARAMETER, "receiveBufferSize", receiveSize);
    logger.log(TCP_CONFIGURED_PARAMETER, "sendBufferSize", sendSize);
    logger.log(TCP_CONFIGURED_PARAMETER, "timeout", socketTimeOut);

    socket.setReceiveBufferSize(receiveSize);
    socket.setSendBufferSize(sendSize);
    socket.setSoTimeout(socketTimeOut);

    socketChannel.configureBlocking(false);
  }

  @Override
  public void completedConnection() {
    int linger = ((TcpConfigDTO)getConfig().getEndPointConfig()).getSoLingerDelaySec();
    try {
      socket.setSoLinger(true, linger);
    } catch (SocketException e) {
      logger.log(TCP_CONFIGURED_PARAMETER, "Unable to set soLinger to " + linger, e);
    }
  }
}
