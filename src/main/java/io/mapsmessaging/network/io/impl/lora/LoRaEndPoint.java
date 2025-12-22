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

package io.mapsmessaging.network.io.impl.lora;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.lora.device.LoRaChipDevice;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDatagram;
import io.mapsmessaging.network.io.impl.lora.stats.LoRaClientStats;
import io.mapsmessaging.utilities.stats.StatsFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoRaEndPoint extends EndPoint {

  private final AtomicBoolean isQueued;

  @Getter
  private final int nodeId;

  private int lastRSSI;
  private Selectable selectable;

  private final LoRaChipDevice loRaDevice;
  private final Queue<LoRaDatagram> incoming;
  private final EndPointJMX mbean;
  private final LinkedHashMap<Integer, LoRaClientStats> clientStats;

  public LoRaEndPoint(LoRaChipDevice loRaDevice, long id, EndPointServer server, EndPointManagerJMX managerMBean) throws IOException {
    super(id, server);
    isQueued = new AtomicBoolean(false);
    this.loRaDevice = loRaDevice;
    nodeId = (int) id;
    clientStats = new LinkedHashMap<>();
    incoming = new LinkedList<>();
    loRaDevice.registerEndPoint(this);
    mbean = new EndPointJMX(managerMBean.getTypePath(), this);
    jmxParentPath = mbean.getTypePath();
    selectable = null;
    name = "LoRa_" + nodeId;
  }

  @Override
  public void close() throws IOException {
    mbean.close();
    super.close();
  }

  public int getRSSI() {
    return lastRSSI;
  }

  @Override
  public boolean isUDP() {
    return true;
  }

  @Override
  public String getProtocol() {
    return "lora";
  }


  public int getIncomingQueueSize(){
    return incoming.size();
  }

  public int getConnectionSize(){
    return clientStats.size();
  }

  public List<LoRaClientStats> getStats() {
    return new ArrayList<>(clientStats.values());
  }

  @Override
  public int sendPacket(Packet packet) {
    InetSocketAddress inetSocketAddress = (InetSocketAddress) packet.getFromAddress();
    byte[] ipAddress = inetSocketAddress.getAddress().getAddress();
    int len = packet.available();
    byte[] buffer = new byte[len];
    packet.get(buffer);
    byte to = ipAddress[3];
    LoRaClientStats stats = getStats(to);
    stats.setLastWriteTime(System.currentTimeMillis());
    loRaDevice.write(buffer, len, (byte) (nodeId & 0xff), to);
    updateWriteBytes(len);
    return len;
  }

  @Override
  public int readPacket(Packet packet) {
    int read = 0;
    synchronized (this) {
      LoRaDatagram datagram = incoming.poll();
      if (datagram != null) {
        byte[] buffer = datagram.getBuffer();
        packet.put(buffer, 0, buffer.length);
        SocketAddress socketAddress = getSocketAddress(datagram.getFrom());
        packet.setFromAddress(socketAddress);
        read = buffer.length;
        updateReadBytes(read);
      }
    }
    return read;
  }

  public SocketAddress getSocketAddress(int loraId) {

    // Reserved Link Local IP Address which is really what Lora Wan is in this case
    byte[] mythical = {(byte) 169, (byte) 254, (byte) (nodeId & 0xff), (byte) loraId};
    try {
      InetAddress inetAddress = InetAddress.getByAddress(mythical);
      return new InetSocketAddress(inetAddress, 1);
    } catch (UnknownHostException e) {
      // Ignore, since we know this is OK
    }
    return null;
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) {
    logger.log(ServerLogMessages.LORA_REGISTER_NETWORK_ACTIVITY, selectionKey);
    selectable = runner;
    if ((selectionKey & SelectionKey.OP_READ) != 0) {
      SimpleTaskScheduler.getInstance().submit(new LoRaReader());
    }
    if ((selectionKey & SelectionKey.OP_WRITE) != 0) {
      SimpleTaskScheduler.getInstance().submit(new LoRaWriter(runner));
    }
    return null;
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return getConfig().getAuthenticationRealm();
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(LoRaEndPoint.class);
  }

  @Override
  public String getRemoteSocketAddress() {
    return "";
  }

  public int getDatagramSize() {
    return loRaDevice.getPacketSize();
  }

  public void queue(LoRaDatagram datagram) {
    synchronized (this) {
      lastRSSI = datagram.getRssi();
      incoming.add(datagram);
      if (!isQueued.get() && selectable != null) {
        isQueued.set(true);
        register(SelectionKey.OP_READ, selectable);
      }
      logger.log(ServerLogMessages.LORA_QUEUED_EVENT, incoming.size(), selectable != null);
      int from = datagram.getFrom();
      LoRaClientStats stats = getStats(from);
      stats.update(datagram);
    }
  }

  private LoRaClientStats getStats(int from) {
    return clientStats.computeIfAbsent(from, f -> new LoRaClientStats(jmxParentPath, f, StatsFactory.getDefaultType()));
  }

  public class LoRaReader implements Runnable {

    public void run() {
      try {
        synchronized (LoRaEndPoint.this) {
          if (incoming.isEmpty()) {
            return; // Nothing to do
          }
        }
        selectable.selected(selectable, null, SelectionKey.OP_READ);
      } finally {
        synchronized (LoRaEndPoint.this) {
          if (!incoming.isEmpty()) {
            register(SelectionKey.OP_READ, selectable);
          } else {
            isQueued.set(false);
          }
        }
      }
    }
  }

  public static class LoRaWriter implements Runnable {

    private final Selectable runner;

    public LoRaWriter(Selectable selectable) {
      runner = selectable;
    }

    public void run() {
      runner.selected(runner, null, SelectionKey.OP_WRITE);
    }
  }

}
