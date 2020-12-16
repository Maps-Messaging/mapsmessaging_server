/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.loragateway;

import static org.maps.network.protocol.impl.loragateway.Constants.DATA;
import static org.maps.network.protocol.impl.loragateway.Constants.VERSION;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.message.Message;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Packet;
import org.maps.network.io.impl.SelectorCallback;
import org.maps.network.io.impl.SelectorTask;
import org.maps.network.io.impl.lora.stats.LoRaClientStats;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.loragateway.handler.DataHandlerFactory;
import org.maps.network.protocol.impl.loragateway.handler.PacketHandler;
import org.maps.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class LoRaProtocol extends ProtocolImpl {

  public final SelectorCallback protocolInterfaceManager;
  private final Logger logger;
  private final SelectorTask selectorTask;
  private final byte address;

  private final LoRaProtocolEndPoint loraProtocolEndPoint;
  private final int transmissionRate;
  private final AtomicInteger transmitCount;
  private final DataHandlerFactory dataHandler;
  private final byte[] configBuffer;
  private volatile boolean closed;
  private boolean sentConfig = false;
  private boolean started = false;
  private boolean sentVersion = false;
  private Future<Runnable> rateResetFuture;
  private final LinkedHashMap<Integer, LoRaClientStats> clientStats;


  public LoRaProtocol(EndPoint endPoint) throws IOException {
    super(new LoRaProtocolEndPoint(endPoint));
    logger = LoggerFactory.getLogger("LoRa Gateway on " + endPoint.getName());
    dataHandler = new DataHandlerFactory();
    Map<String, String> parameters = endPoint.getServer().getUrl().getParameters();
    byte power = loadPower(parameters);
    address = loadAddress(parameters);
    byte[] key = loadKey(parameters);

    clientStats = new LinkedHashMap<>();
    loraProtocolEndPoint = (LoRaProtocolEndPoint) getEndPoint();
    loraProtocolEndPoint.setProtocol(this);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), true);
    protocolInterfaceManager = new MQTTSNInterfaceManager((byte) 1, selectorTask, getEndPoint());
    loraProtocolEndPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());

    transmissionRate = endPoint.getConfig().getProperties().getIntProperty("LoRaMaxTransmissionRate", DefaultConstants.LORA_MAXIMUM_TX_RATE);
    transmitCount = new AtomicInteger(transmissionRate);

    configBuffer = new byte[18];
    configBuffer[0] = address;
    configBuffer[1] = power;
    System.arraycopy(key, 0, configBuffer, 2, configBuffer.length - 2);
    sendCommand(VERSION); // Request version of gateway
    closed = false;
    rateResetFuture = SimpleTaskScheduler.getInstance().schedule(new RateReset(), DefaultConstants.RATE_RESET_INTERVAL, TimeUnit.MILLISECONDS);
  }

  @Override
  public void close() throws IOException {
    closed = true;
    for(LoRaClientStats stats:clientStats.values()){
      stats.close();
    }
    rateResetFuture.cancel(false);
    super.close();
  }

  public void sendCommand(byte command) throws IOException {
    sendCommand(command, (byte) 0, null);
  }

  public void sendCommand(byte command, byte len, byte[] data) throws IOException {
    Packet initPacket = new Packet(2 + len, false);
    initPacket.put(command);
    initPacket.put(len);
    if (len > 0) {
      initPacket.put(data, 0, len);
    }
    initPacket.flip();
    loraProtocolEndPoint.sendPackedPacket(initPacket);
  }

  int handlePacket(Packet packet) throws IOException {
    sentMessage();
    InetSocketAddress inetSocketAddress = (InetSocketAddress) packet.getFromAddress();
    byte[] ipAddress = inetSocketAddress.getAddress().getAddress();
    Packet out = new Packet(packet.available() + 5, false);
    out.put(DATA); // DATA
    out.put((byte) packet.available()); // Length of Packet
    out.put(ipAddress[3]); // Client ID
    out.put(packet);
    out.flip();
    selectorTask.register(SelectionKey.OP_READ);
    if (transmitCount.decrementAndGet() == 0) {
      logger.log(LogMessages.LORA_GATEWAY_EXCEED_RATE, transmissionRate);
    }
    return loraProtocolEndPoint.sendPackedPacket(out);
  }

  @Override
  public synchronized boolean processPacket(Packet packet) throws IOException {
    try {
      receivedMessage();
      int command = packet.get();
      int len = packet.get();
      PacketHandler handler = dataHandler.getHandler((byte) command);
      if (handler.processPacket(this, packet, len, logger)) {
        endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
      }
    } catch (IOException e) {
      logger.log(LogMessages.LORA_GATEWAY_EXCEPTION, e);
      throw e;
    }
    return true;
  }

  @Override
  public String getName() {
    return "LoRa MQTT-SN GW";
  }

  @Override
  public String getSessionId() {
    return endPoint.getName() + " " + getName();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  public SocketAddress getSocketAddress(int loraId) {

    // Reserved Link Local IP Address which is really what Lora Wan is in this case
    byte[] mythical = {(byte) 169, (byte) 254, address, (byte) loraId};
    try {
      InetAddress inetAddress = InetAddress.getByAddress(mythical);
      return new InetSocketAddress(inetAddress, 1);
    } catch (UnknownHostException e) {
      // Ignore, since we know this is OK
    }
    return null;
  }

  private byte loadPower(Map<String, String> parameters) {
    int tmpPower = 20; // Max Power
    if (parameters.containsKey("power")) {
      String tmp = parameters.get("power");
      tmpPower = Integer.parseInt(tmp);
    }
    return (byte) tmpPower;
  }

  private byte loadAddress(Map<String, String> parameters) {
    int tmpAddress = 1;
    if (parameters.containsKey("address")) {
      String tmp = parameters.get("address");
      tmpAddress = Integer.parseInt(tmp);
    }
    return (byte) (tmpAddress & 0xff);
  }

  private byte[] loadKey(Map<String, String> parameters) {
    byte[] key = new byte[16]; // Key size is 16 bytes or 128 bit key
    String encodedKey = parameters.get("key");
    if (encodedKey != null) {
      StringTokenizer st = new StringTokenizer(encodedKey, ","); // Comma separated list
      int index = 0;
      while (index < key.length && st.hasMoreElements()) {
        String entry = st.nextElement().toString().trim();
        int radix = 10;
        if (entry.startsWith("0x")) {
          entry = entry.substring(2);
          radix = 16;
        }
        int value = Integer.parseInt(entry, radix);
        key[index] = (byte) (value & 0xff);
        index++;
      }
    }
    return key;
  }

  @Override
  public void sendMessage(@NotNull Destination destination, @NotNull SubscribedEventManager subscription, @NotNull Message message, @NotNull Runnable completionTask) {
    // This should not be called since this protocol is NOT registered with the messaging engine
  }

  @Override
  public void sendKeepAlive() {
    // This should not be called since this protocol is NOT registered with the messaging engine
  }

  public byte[] getConfigBuffer() {
    return configBuffer;
  }

  public boolean isSentConfig() {
    return sentConfig;
  }

  public void setSentConfig(boolean sentConfig) {
    this.sentConfig = sentConfig;
  }

  public boolean isStarted() {
    return started;
  }

  public void setStarted(boolean started) {
    this.started = started;
  }

  public boolean isSentVersion() {
    return sentVersion;
  }

  public void setSentVersion(boolean sentVersion) {
    this.sentVersion = sentVersion;
  }

  public void handleIncomingPacket(Packet packet, int clientId, int rssi) throws IOException {
    LoRaClientStats stats = clientStats.computeIfAbsent(clientId, f -> new LoRaClientStats(endPoint.getJMXTypePath(), f));
    stats.update(clientId, rssi);
    protocolInterfaceManager.processPacket(packet);
  }

  private final class RateReset implements Runnable {

    @Override
    public void run() {
      if (!closed) {
        transmitCount.set(transmissionRate);
        rateResetFuture = SimpleTaskScheduler.getInstance().schedule(new RateReset(), DefaultConstants.RATE_RESET_INTERVAL, TimeUnit.MILLISECONDS);
      }
    }
  }
}