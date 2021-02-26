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

package org.maps.network.io.impl.serial;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class SerialEndPortScanner implements Runnable {

  private static final SerialEndPortScanner instance = new SerialEndPortScanner();

  private final Logger logger;
  private final Map<String, SerialEndPointServer> serverEndPoints;
  private final Map<String, SerialPort> knownPorts;


  private SerialEndPortScanner() {
    knownPorts = new TreeMap<>();
    serverEndPoints = new LinkedHashMap<>();
    logger = LoggerFactory.getLogger(SerialEndPortScanner.class);
    run();
  }

  public static SerialEndPortScanner getInstance() {
    return instance;
  }

  public synchronized SerialPort add(String port, SerialEndPointServer server) {
    serverEndPoints.put(port.toLowerCase(), server);
    return knownPorts.get(port.toLowerCase());
  }

  public synchronized void del(String port) {
    serverEndPoints.remove(port);
  }

  private synchronized void scanForPort() {
    SerialPort[] ports = SerialPort.getCommPorts();
    scanPorts(ports);
    if (ports.length != knownPorts.size()) {
      scanForChanges(ports);
    }
  }

  // While knownPorts.computeIfAbsent could be used, the following logic being performed
  // on the addition of a port seems rather large and the logic flows better as is
  @java.lang.SuppressWarnings("java:S3824")
  private void scanPorts(SerialPort[] ports) {
    for (SerialPort port : ports) {
      String key = port.getSystemPortName().toLowerCase();
      if (!knownPorts.containsKey(key)) {
        knownPorts.put(key, port);
        SerialEndPointServer server = serverEndPoints.get(key);
        if (server != null) {
          logger.log(LogMessages.SERIAL_PORT_SCANNER_BINDING, server.getName(), key);
          try {
            server.bind(port);
          } catch (IOException e) {
            logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
          }
        } else {
          logger.log(LogMessages.SERIAL_PORT_SCANNER_UNUSED, key);
        }
      }
    }
  }

  private void scanForChanges(SerialPort[] ports){
    List<String> currentPorts = new ArrayList<>(knownPorts.keySet());
    for (String current : currentPorts) {
      boolean found = false;
      for (SerialPort port : ports) {
        String key = port.getSystemPortName().toLowerCase();
        if (current.equals(key)) {
          found = true;
          break;
        }
      }
      if (!found) {
        handleNewPorts(current);
      }
    }
  }

  private void handleNewPorts(String current){
    SerialEndPointServer server = serverEndPoints.get(current);
    SerialPort port = knownPorts.remove(current);
    if (server != null &&
        port != null) {
      logger.log(LogMessages.SERIAL_PORT_SCANNER_UNBINDING, server.getName(), current);
      try {
        server.unbind(port);
      } catch (IOException e) {
        logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
    } else {
      logger.log(LogMessages.SERIAL_PORT_SCANNER_LOST, current);
    }
  }

  @Override
  public void run() {
    scanForPort();
    SimpleTaskScheduler.getInstance().schedule(this, 5, TimeUnit.SECONDS);
  }
}
