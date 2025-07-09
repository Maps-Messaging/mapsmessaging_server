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

package io.mapsmessaging.network.io;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolInfo;
import io.mapsmessaging.utilities.stats.StatsFactory;
import lombok.Getter;
import lombok.Setter;

import javax.security.auth.Subject;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public abstract class EndPoint implements Closeable {


  public static final LongAdder totalReceived = new LongAdder();
  public static final LongAdder totalSent = new LongAdder();
  public static final LongAdder currentConnections = new LongAdder();
  public static final LongAdder totalReadBytes = new LongAdder();
  public static final LongAdder totalWriteBytes = new LongAdder();
  public static final LongAdder totalConnections = new LongAdder();
  public static final LongAdder totalDisconnections = new LongAdder();

  private final AtomicLong lastRead = new AtomicLong();
  private final AtomicLong lastWrite = new AtomicLong();
  protected List<String> jmxParentPath;

  @Getter
  private final boolean isClient;

  @Getter
  @Setter
  protected ProxyProtocolInfo proxyProtocolInfo;

  @Getter
  private boolean isClosed;

  @Getter
  protected final EndPointServerStatus server;
  @Getter
  private final EndPointStatus endPointStatus;
  @Getter
  protected final Logger logger;
  @Getter
  private final long id;
  @Getter
  private final long connected = System.currentTimeMillis();
  @Setter
  private CloseHandler closeHandler;

  @Getter
  @Setter
  private Protocol boundProtocol;

  @Getter
  protected String name;

  protected EndPoint(long id, EndPointServerStatus server) {
    this.server = server;
    isClient = !(server instanceof EndPointServer);
    this.id = id;
    endPointStatus = new EndPointStatus(StatsFactory.getDefaultType());
    logger = createLogger();
    totalConnections.increment();
    currentConnections.increment();
    isClosed = false;
  }

  @Override
  public void close() throws IOException {
    if (closeHandler != null) {
      closeHandler.close();
    }
    synchronized (this) {
      if (!isClosed) {
        isClosed = true;
        totalDisconnections.increment();
        currentConnections.decrement();
      }
    }
  }

  public void updateReadBytes(int read) {
    endPointStatus.updateReadBytes(read);
    totalReadBytes.add(read);
    lastRead.set(System.currentTimeMillis());
    if (server != null) {
      server.updateBytesRead(read);
      server.incrementPacketsRead();
    }
  }

  public void updateWriteBytes(int wrote) {
    endPointStatus.updateWriteBytes(wrote);
    totalWriteBytes.add(wrote);
    lastWrite.set(System.currentTimeMillis());
    if (server != null) {
      server.updateBytesSent(wrote);
      server.incrementPacketsSent();
    }
  }

  public void completedConnection() {

  }

  public boolean isProxyAllowed() {
    return true;
  }

  public EndPointServerConfigDTO getConfig() {
    return server.getConfig();
  }

  public List<String> getJMXTypePath() {
    return jmxParentPath;
  }

  public boolean isUDP() {
    return false;
  }

  public long getLastRead() {
    return lastRead.get();
  }

  public long getLastWrite() {
    return lastWrite.get();
  }

  public Subject getEndPointSubject() {
    if(boundProtocol != null){
      return boundProtocol.getSubject();
    }
    return null;
  }

  public Principal getEndPointPrincipal() {
    return null;
  }

  public boolean isSSL() {
    return false;
  }

  public abstract String getProtocol();

  public abstract int sendPacket(Packet packet) throws IOException;

  public abstract int readPacket(Packet packet) throws IOException;

  public abstract FutureTask<SelectionKey> register(int selectionKey, Selectable runner)
      throws IOException;

  public abstract FutureTask<SelectionKey> deregister(int selectionKey)
      throws ClosedChannelException;

  public abstract String getAuthenticationConfig();

  protected abstract Logger createLogger();

}
