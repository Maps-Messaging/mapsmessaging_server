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

import java.util.*;

public class SerialPortRegistry {
  private final Map<SerialPortInfo, SerialPort> knownPorts = new TreeMap<>();

  public synchronized SerialPort getByName(String portName) {
    return knownPorts.entrySet().stream()
        .filter(e -> e.getKey().getName().equalsIgnoreCase(portName))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  public synchronized SerialPort getBySerial(String serial) {
    return knownPorts.entrySet().stream()
        .filter(e -> e.getKey().getSerialNumber().equalsIgnoreCase(serial))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  public synchronized void add(SerialPortInfo info, SerialPort port) {
    knownPorts.put(info, port);
  }

  public synchronized SerialPort remove(String portName) {
    Iterator<Map.Entry<SerialPortInfo, SerialPort>> iter = knownPorts.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<SerialPortInfo, SerialPort> entry = iter.next();
      if (entry.getKey().getName().equalsIgnoreCase(portName)) {
        iter.remove();
        return entry.getValue();
      }
    }
    return null;
  }

  public synchronized List<SerialPortInfo> listInfos() {
    return new ArrayList<>(knownPorts.keySet());
  }

  public synchronized int size() {
    return knownPorts.size();
  }
}
