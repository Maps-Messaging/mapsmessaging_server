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

package org.maps.network.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.maps.logging.Logger;
import org.maps.network.NetworkConfig;
import org.maps.utilities.stats.LinkedMovingAverages;
import org.maps.utilities.stats.MovingAverageFactory;
import org.maps.utilities.stats.MovingAverageFactory.ACCUMULATOR;

public abstract class EndPoint implements Closeable {

  public static final LongAdder totalReadBytes = new LongAdder();
  public static final LongAdder totalWriteBytes = new LongAdder();
  public static final LongAdder totalConnections = new LongAdder();
  public static final LongAdder totalDisconnections = new LongAdder();

  protected final EndPointServerStatus server;
  protected final Logger logger;

  private final AtomicLong lastRead = new AtomicLong();
  private final AtomicLong lastWrite = new AtomicLong();

  private final LinkedMovingAverages readByteAverages;
  private final LinkedMovingAverages writeByteAverages;

  private final LinkedMovingAverages bufferOverFlow;
  private final LinkedMovingAverages bufferUnderFlow;

  private final boolean isClient;
  private final long id;

  protected List<String> jmxParentPath;
  private CloseHandler closeHandler;

  protected EndPoint(long id, EndPointServerStatus server) {
    this.server = server;
    isClient = !(server instanceof EndPointServer);
    this.id = id;

    readByteAverages = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Read Bytes", 1, 5, 4, TimeUnit.MINUTES, "Bytes");
    writeByteAverages = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Write Bytes", 1, 5, 4, TimeUnit.MINUTES, "Bytes");
    bufferOverFlow = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Buffer Overflow", 1, 5, 4, TimeUnit.MINUTES, "Packets");
    bufferUnderFlow = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Buffer Underflow", 1, 5, 4, TimeUnit.MINUTES, "Packets");
    logger = createLogger();
    totalConnections.increment();
  }

  public void close() throws IOException {
    if (closeHandler != null) {
      closeHandler.close();
    }
    totalDisconnections.increment();
  }

  public boolean isClient(){
    return isClient;
  }

  public void updateReadBytes(int read) {
    readByteAverages.add(read);
    totalReadBytes.add(read);
    lastRead.set(System.currentTimeMillis());
    if(server != null) {
      server.updateBytesRead(read);
      server.incrementPacketsRead();
    }
  }

  public void updateWriteBytes(int wrote) {
    writeByteAverages.add(wrote);
    totalWriteBytes.add(wrote);
    lastWrite.set(System.currentTimeMillis());
    if(server != null) {
      server.updateBytesSent(wrote);
      server.incrementPacketsSent();
    }
  }

  public NetworkConfig getConfig() {
    return server.getConfig();
  }

  public List<String> getJMXTypePath() {
    return jmxParentPath;
  }

  public void setCloseHandler(CloseHandler closeHandler) {
    this.closeHandler = closeHandler;
  }

  public long getId() {
    return id;
  }

  public boolean isUDP() {
    return false;
  }

  public LinkedMovingAverages getReadBytes() {
    return readByteAverages;
  }

  public LinkedMovingAverages getWriteBytes() {
    return writeByteAverages;
  }

  public long getLastRead() {
    return lastRead.get();
  }

  public long getLastWrite() {
    return lastWrite.get();
  }

  public EndPointServerStatus getServer() {
    return server;
  }

  public Logger getLogger() {
    return logger;
  }

  public Principal getEndPointPrincipal() {
    return null;
  }

  public boolean isSSL() {
    return false;
  }

  public LinkedMovingAverages getOverFlow() {
    return bufferOverFlow;
  }

  public LinkedMovingAverages getUnderFlow() {
    return bufferUnderFlow;
  }

  public long getOverFlowTotal(){
    return bufferOverFlow.getTotal();
  }

  public long getUnderFlowTotal(){
    return bufferUnderFlow.getTotal();
  }

  public void incrementOverFlow() {
    bufferOverFlow.increment();
  }

  public void incrementUnderFlow() {
    bufferUnderFlow.increment();
  }

  public abstract String getProtocol();

  public abstract int sendPacket(Packet packet) throws IOException;

  public abstract int readPacket(Packet packet) throws IOException;

  public abstract FutureTask<SelectionKey> register(int selectionKey, Selectable runner)
      throws IOException;

  public abstract FutureTask<SelectionKey> deregister(int selectionKey)
      throws ClosedChannelException;

  public abstract String getAuthenticationConfig();

  public abstract String getName();

  protected abstract Logger createLogger();

}
