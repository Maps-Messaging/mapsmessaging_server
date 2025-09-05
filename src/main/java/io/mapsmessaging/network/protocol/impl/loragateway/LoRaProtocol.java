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

package io.mapsmessaging.network.protocol.impl.loragateway;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.network.impl.LoRaConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.LoRaProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.LoraProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.lora.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.LoRaDeviceManager;
import io.mapsmessaging.network.io.impl.lora.serial.LoRaProtocolEndPoint;
import io.mapsmessaging.network.io.impl.lora.stats.LoRaClientStats;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.loragateway.handler.DataHandlerFactory;
import io.mapsmessaging.network.protocol.impl.loragateway.handler.PacketHandler;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTTSNInterfaceManager;
import io.mapsmessaging.utilities.stats.StatsFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mapsmessaging.network.protocol.impl.loragateway.Constants.DATA;
import static io.mapsmessaging.network.protocol.impl.loragateway.Constants.VERSION;

public class LoRaProtocol extends Protocol {

  public final SelectorCallback protocolInterfaceManager;
  private final Logger logger;
  private final SelectorTask selectorTask;
  @Getter
  private final byte address;

  private final LoRaProtocolEndPoint loraProtocolEndPoint;
  private final int transmissionRate;
  private final AtomicInteger transmitCount;
  private final DataHandlerFactory dataHandler;

  @Getter
  private final byte[] configBuffer;
  private volatile boolean closed;
  @Getter
  @Setter
  private boolean sentConfig = false;
  @Getter
  @Setter
  private boolean started = false;
  @Getter
  @Setter
  private boolean sentVersion = false;

  private Future<?> rateResetFuture;

  private final LinkedHashMap<Integer, LoRaClientStats> clientStats;


  public LoRaProtocol(EndPoint endPoint) throws IOException {
    super(new LoRaProtocolEndPoint(endPoint), new LoRaProtocolConfigDTO());
    logger = LoggerFactory.getLogger("LoRa Gateway on " + endPoint.getName());
    dataHandler = new DataHandlerFactory();
    EndPointURL endPointURL = new EndPointURL(endPoint.getConfig().getUrl());
    LoRaDevice loRaDevice = LoRaDeviceManager.getInstance().getDevice(endPointURL);
    LoRaConfigDTO loRaConfig = loRaDevice.getConfig();
    byte power = (byte) loRaConfig.getPower();
    address = (byte) loRaConfig.getAddress();
    byte[] key = loadKey(loRaConfig.getHexKey());

    clientStats = new LinkedHashMap<>();
    loraProtocolEndPoint = (LoRaProtocolEndPoint) getEndPoint();
    loraProtocolEndPoint.setProtocol(this);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), true);
    protocolInterfaceManager = new MQTTSNInterfaceManager((byte) 1, selectorTask, endPoint);
    loraProtocolEndPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());

    transmissionRate = loRaConfig.getTransmissionRate() < 0 ? 1000 : loRaConfig.getTransmissionRate();
    transmitCount = new AtomicInteger(transmissionRate);

    configBuffer = new byte[18];
    configBuffer[0] = address;
    configBuffer[1] = power;
    System.arraycopy(key, 0, configBuffer, 2, configBuffer.length - 2);
    sendCommand(VERSION); // Request the version of gateway
    closed = false;
    rateResetFuture = SimpleTaskScheduler.getInstance().schedule(new RateReset(), DefaultConstants.RATE_RESET_INTERVAL, TimeUnit.MILLISECONDS);
  }

  @Override
  public void close() throws IOException {
    closed = true;
    for (LoRaClientStats stats : clientStats.values()) {
      stats.close();
    }
    if(rateResetFuture != null) {
      rateResetFuture.cancel(false);
    }
    super.close();
  }

  @Override
  public Subject getSubject() {
    return null;
  }

  public List<LoRaClientStats> getClientStats(){
    return new ArrayList<>(clientStats.values());
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

  public int handlePacket(Packet packet) throws IOException {
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
      logger.log(ServerLogMessages.LORA_GATEWAY_EXCEED_RATE, transmissionRate);
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
      logger.log(ServerLogMessages.LORA_GATEWAY_EXCEPTION, e);
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

  private byte[] loadKey(String encodedKey) {
    byte[] key = new byte[16]; // Key size is 16 bytes or 128 bit key
    if (encodedKey != null && !encodedKey.isEmpty()) {
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
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    // This should not be called since this protocol is NOT registered with the messaging engine
  }

  @Override
  public void sendKeepAlive() {
    // This should not be called since this protocol is NOT registered with the messaging engine
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    LoraProtocolInformation information = new LoraProtocolInformation();
    updateInformation(information);
    return information;
  }

  public void handleIncomingPacket(Packet packet, int clientId, int rssi) throws IOException {
    LoRaClientStats stats = clientStats.computeIfAbsent(clientId, f -> new LoRaClientStats(endPoint.getJMXTypePath(), f, StatsFactory.getDefaultType()));
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