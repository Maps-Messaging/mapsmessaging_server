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
package io.mapsmessaging.network.io.impl.serial.management;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("java:S6548") // yes, it is a singleton
public class SerialPortScanner implements Runnable {

  private static class Holder {
    static final SerialPortScanner INSTANCE = new SerialPortScanner();
  }

  public static SerialPortScanner getInstance() {
    return Holder.INSTANCE;
  }

  private final Logger logger;
  private final SerialPortRegistry portRegistry;
  private final SerialPortListenerRegistry listenerRegistry;

  private SerialPortScanner() {
    logger = LoggerFactory.getLogger(SerialPortScanner.class);
    portRegistry = new SerialPortRegistry();
    listenerRegistry = new SerialPortListenerRegistry();
    run();
  }

  public boolean isConnected(String serialName) {
    return portRegistry.getByName(serialName) != null;
  }

  public boolean isConnectedBySerial(String serialNumber) {
    return portRegistry.getBySerial(serialNumber) != null;
  }

  public SerialPort allocatePort(String name) {
    return portRegistry.getByName(name);
  }

  public SerialPort allocatePortBySerial(String serial) {
    return portRegistry.getBySerial(serial);
  }

  public SerialPort add(String port, SerialPortListener server) {
    listenerRegistry.add(new SerialPortInfo(port, ""), server);
    return portRegistry.getByName(port);
  }

  public SerialPort addBySerial(String serial, SerialPortListener server) {
    listenerRegistry.add(new SerialPortInfo("", serial), server);
    return portRegistry.getBySerial(serial);
  }


  public void del(String port) {
    listenerRegistry.remove(port);
  }

  @Override
  public void run() {
    scanForPort();
    SimpleTaskScheduler.getInstance().schedule(this, 5, TimeUnit.SECONDS);
  }

  private void scanForChanges(SerialPort[] ports) {
    List<SerialPortInfo> currentPorts = portRegistry.listInfos();
    for (SerialPortInfo current : currentPorts) {
      boolean found = false;
      for (SerialPort port : ports) {
        String key = port.getSystemPortName().toLowerCase();
        String serialNumber = port.getSerialNumber().toLowerCase();
        if (current.getSerialNumber().equals(serialNumber) || current.getName().equals(key)) {
          found = true;
          break;
        }
      }
      if (!found) {
        handleRemovedPort(current);
      }
    }
  }

  private void handleRemovedPort(SerialPortInfo current) {
    SerialPortListener listener = listenerRegistry.get(current);
    SerialPort port = portRegistry.remove(current.getName());
    if (listener != null && port != null) {
      logger.log(ServerLogMessages.SERIAL_PORT_SCANNER_UNBINDING, listener.getName(), current);
      try {
        listener.unbind(port);
      } catch (IOException e) {
        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
    } else {
      logger.log(ServerLogMessages.SERIAL_PORT_SCANNER_LOST, current);
    }
    listenerRegistry.remove(current);
  }

  private void scanPorts(SerialPort[] devices) {
    for (SerialPort serialDevice : devices) {
      String key = serialDevice.getSystemPortName().toLowerCase();
      if (portRegistry.getByName(key) == null) {
        String serialNumber = serialDevice.getSerialNumber();
        if (serialNumber == null) {
          serialNumber = key;
        }
        SerialPortInfo info = new SerialPortInfo(key, serialNumber);
        portRegistry.add(info, serialDevice);
        SerialPortListener server = listenerRegistry.find(key, serialNumber);
        if (server != null) {
          logger.log(ServerLogMessages.SERIAL_PORT_SCANNER_BINDING, server.getName(), key);
          try {
            server.bind(serialDevice);
          } catch (IOException e) {
            logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
          }
        } else {
          logger.log(ServerLogMessages.SERIAL_PORT_SCANNER_UNUSED, key);
        }
      }
    }
  }

  private void scanForPort() {
    SerialPort[] ports = SerialPort.getCommPorts();
    scanPorts(ports);
    if (ports.length != portRegistry.size()) {
      scanForChanges(ports);
    }
  }
}
