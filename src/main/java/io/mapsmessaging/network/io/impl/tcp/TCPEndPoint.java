/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.tcp;

import static io.mapsmessaging.logging.LogMessages.TCP_ACCEPT_START;
import static io.mapsmessaging.logging.LogMessages.TCP_CLOSE_ERROR;
import static io.mapsmessaging.logging.LogMessages.TCP_CONFIGURED_PARAMETER;
import static io.mapsmessaging.logging.LogMessages.TCP_READ_BUFFER;
import static io.mapsmessaging.logging.LogMessages.TCP_SEND_BUFFER;

import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPEndPoint extends EndPoint {

  protected final Socket socket;
  protected final SocketChannel socketChannel;
  protected final Selector selector;
  private final String authenticationConfig;
  private final String name;
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
      if(isClient()) {
        name = getProtocol() + "_" + socket.getLocalAddress().toString()+"_"+socket.getLocalPort();
      }
      else{
        name = getProtocol() + "_" + socket.getRemoteSocketAddress().toString();
      }
      configure(endPointServerStatus.getConfig().getProperties());
    } catch (IOException e) {
      logger.log(LogMessages.TCP_CONNECT_FAILED, e, accepted.toString());
      throw e;
    }
    mbean = new EndPointJMX(jmxParent, this);
    jmxParentPath = mbean.getTypePath();
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
      name = getProtocol() + "_" + socket.getRemoteSocketAddress().toString();
      configure(server.getConfig().getProperties());
    } catch (IOException e) {
      logger.log(LogMessages.TCP_CONNECT_FAILED, e, accepted.toString());
      throw e;
    }
    mbean = new EndPointJMX(managerMBean.getTypePath(), this);
    jmxParentPath = mbean.getTypePath();
  }


  @Override
  public void close() {
    if (!isClosed.getAndSet(true)) {
      logger.log(LogMessages.TCP_CONNECTION_CLOSE, name);
      try {
        super.close();
        deregister(-1);
        selector.wakeup();
        socket.shutdownInput();
        socket.shutdownOutput();
        if (!socket.isClosed()) {
          SimpleTaskScheduler.getInstance().schedule(new SocketCloseTask(this), 500, TimeUnit.MILLISECONDS);
        } else {
          socketChannel.close();
          socket.close();
        }
        logger.log(LogMessages.TCP_CLOSE_SUCCESS, name);
      } catch (IOException e) {
        logger.log(LogMessages.TCP_CLOSE_EXCEPTION, e, name);
      } finally {
        mbean.close();
        if(server != null) {
          server.handleCloseEndPoint(this);
        }
      }
    }
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
    if (!socketChannel.isOpen() || isClosed.get()) {
      throw new IOException("Socket closed");
    }
    int count = socketChannel.read(bb);
    if (count < 0) {
      throw new IOException("Socket closed");
    }
    updateReadBytes(count);
    return count;
  }

  @Override
  public String toString() {
    return socketChannel.socket().toString();
  }

  protected Logger createLogger() {
    return LoggerFactory.getLogger(TCPEndPoint.class.getName() + "_" + getId());
  }

  private void configure(ConfigurationProperties config) throws IOException {
    /*
    These are NOT configurable and required for normal operation
     */
    socket.setKeepAlive(true);
    socket.setReuseAddress(true);
    socket.setTcpNoDelay(true);
    socket.setSoLinger(false, 0);

    /*
    Configurable settings for Sockets
     */
    int receiveSize = config.getIntProperty("receiveBufferSize", 128000);
    int sendSize = config.getIntProperty("sendBufferSize", 128000);
    int socketTimeOut = config.getIntProperty("timeout", 120000);

    logger.log(TCP_CONFIGURED_PARAMETER, "receiveBufferSize", receiveSize);
    logger.log(TCP_CONFIGURED_PARAMETER, "sendBufferSize", sendSize);
    logger.log(TCP_CONFIGURED_PARAMETER, "timeout", socketTimeOut);

    socket.setReceiveBufferSize(receiveSize);
    socket.setSendBufferSize(sendSize);
    socket.setSoTimeout(socketTimeOut);

    socketChannel.configureBlocking(false);
  }

  private final class SocketCloseTask implements Runnable {

    private final SocketChannel socketChannel;
    private final Socket socket;

    public SocketCloseTask(TCPEndPoint tcpEndPoint) {
      socket = tcpEndPoint.socket;
      socketChannel = tcpEndPoint.socketChannel;
    }

    @Override
    public void run() {
      try {
        socketChannel.close();
        socket.close();
      } catch (IOException e) {
        logger.log(TCP_CLOSE_ERROR, e);
      }
    }
  }
}
