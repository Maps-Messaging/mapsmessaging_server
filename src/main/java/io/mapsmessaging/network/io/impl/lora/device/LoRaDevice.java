/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.lora.device;

import io.mapsmessaging.config.lora.LoRaDeviceConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.impl.lora.LoRaEndPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;

public class LoRaDevice {

  @Getter
  private final LongAdder packetsSent = new LongAdder();
  @Getter
  private final LongAdder packetsReceived = new LongAdder();
  @Getter
  private final LongAdder bytesReceived = new LongAdder();
  @Getter
  private final LongAdder bytesSent = new LongAdder();
  @Getter
  private final LoRaDeviceConfig config;

  final Logger logger;
  private int radioHandle;

  private final Map<Integer, LoRaEndPoint> registeredEndPoint;
  private boolean isInitialised;
  private PacketReader packetReader;

  protected LoRaDevice(LoRaDeviceConfig config) {
    logger = LoggerFactory.getLogger(LoRaDevice.class);
    this.config = config;
    isInitialised = false;
    registeredEndPoint = new LinkedHashMap<>();
  }

  public synchronized void registerEndPoint(LoRaEndPoint endPoint) throws IOException {
    if (!isInitialised && !init((int) endPoint.getId())) {
      logger.log(ServerLogMessages.LORA_DEVICE_NOT_INITIALISED, config.getName(), config.getRadio());
    } else if (radioHandle < 0) {
      logger.log(ServerLogMessages.LORA_DEVICE_INIT_FAILED, config.getName(), config.getRadio());
    } else {
      if (!registeredEndPoint.containsKey((int) endPoint.getId())) {
        registeredEndPoint.put((int) endPoint.getId(), endPoint);
        logger.log(ServerLogMessages.LORA_DEVICE_REGISTERED, endPoint.getName(), config.getName());
      }
    }
  }

  public void close() {
    packetReader.close();
  }

  public String getName() {
    return config.getName();
  }

  private boolean init(int addr) throws IOException {
    isInitialised = true;
    int result = init(addr, config.getFrequency(), config.getCs(), config.getIrq(), config.getRst());
    if (result >= 0) {
      setPower(result, config.getPower(), false);
      if (config.getCadTimeout() > 0) {
        setCADTimeout(result, config.getCadTimeout());
      }
      setPromiscuous(radioHandle, true);
      packetReader = new PacketReader(this);
    }
    radioHandle = result;
    return radioHandle >= 0;
  }

  public void log(String message) {
    logger.log(ServerLogMessages.LORA_DEVICE_DRIVER_LOG, config.getName(), config.getRadio(), message);
  }

  public LoRaEndPoint getEndPoint(int id) {
    return registeredEndPoint.get(id);
  }

  public List<LoRaEndPoint> getEndPoints(){
    return new ArrayList<>(registeredEndPoint.values());
  }

  void handleIncomingPacket(LoRaDatagram datagram) {
    LoRaEndPoint endPoint = registeredEndPoint.get(datagram.getTo());
    if (endPoint != null) {
      packetsReceived.increment();
      endPoint.queue(datagram);
    } else {
      logger.log(ServerLogMessages.LORA_DEVICE_NO_REGISTERED_ENDPOINT, datagram.getTo());
    }
  }

  public synchronized boolean write(byte[] buffer, int length, byte from, byte to) {
    if (radioHandle >= 0) {
      bytesSent.add(length);
      packetsSent.increment();
      return write(radioHandle, buffer, length, from, to);
    }
    return false;
  }

  public synchronized long read(byte[] buffer, int length) {
    if (radioHandle >= 0) {
      long read = read(radioHandle, buffer, length);
      if(read > 0){
        bytesReceived.add(read);
      }
      return read;
    }
    return -1;
  }

  public int available() {
    if (radioHandle >= 0) {
      return available(radioHandle);
    }
    return 0;
  }

  public int getPacketSize() {
    if (radioHandle >= 0) {
      return getPacketSize(radioHandle);
    }
    return 0;
  }

  //
  // Supply the GPIO pins for ChipSelect, Interrupt and Reset
  //
  private native int init(int nodeAddress, float frequency, int cs, int irq, int rst) throws IOException;

  //
  // Set the power output for the radio
  //
  private native boolean setPower(int radioHandle, int power, boolean flag) throws IOException;

  //
  // Set the timeout for idle detection
  //
  private native boolean setCADTimeout(int radioHandle, int timeout) throws IOException;

  //
  // Write the buffer to the host specified
  //
  private native boolean write(int radioHandle, byte[] buffer, int length, byte from, byte to);

  //
  // Read buffer, the return long contains
  //
  // byte[0] = length of buffer
  // byte[1] = to host
  // byte[2] = from host
  // byte[3] = RSSI value
  // byte[4] = future
  // byte[5] = future
  // byte[6] = future
  // byte[7] = future
  private native long read(int radioHandle, byte[] buffer, int length);

  private native int available(int radioHandle);

  private native void setPromiscuous(int radioHandle, boolean flag);

  private native int getPacketSize(int radioHandle);
}
