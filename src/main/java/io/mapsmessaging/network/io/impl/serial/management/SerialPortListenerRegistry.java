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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SerialPortListenerRegistry {
  private final Map<SerialPortInfo, SerialPortListener> listeners = new LinkedHashMap<>();

  public synchronized void add(SerialPortInfo serialPortInfo, SerialPortListener listener) {
    listeners.put(serialPortInfo, listener);
  }

  public synchronized void remove(String port) {
    listeners.keySet().removeIf(info -> info.getName().equalsIgnoreCase(port));
  }

  public synchronized SerialPortListener find(String port, String serialNumber) {
    return listeners.entrySet().stream()
        .filter(e -> e.getKey().getSerialNumber().equalsIgnoreCase(serialNumber) ||
            e.getKey().getName().equalsIgnoreCase(port))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  public synchronized SerialPortListener get(SerialPortInfo info) {
    return listeners.get(info);
  }

  public synchronized void remove(SerialPortInfo info) {
    listeners.remove(info);
  }

  public synchronized Set<Map.Entry<SerialPortInfo, SerialPortListener>> entrySet() {
    return new LinkedHashSet<>(listeners.entrySet());
  }
}
