/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.network.monitor;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import lombok.Getter;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.mapsmessaging.logging.ServerLogMessages.NETWORK_MONITOR_EXCEPTION;
import static io.mapsmessaging.logging.ServerLogMessages.NETWORK_MONITOR_STATE_CHANGE;

public class NetworkInterfaceMonitor implements Agent {

  @Getter
  private static final NetworkInterfaceMonitor instance = new NetworkInterfaceMonitor();

  private final Logger logger = LoggerFactory.getLogger(NetworkInterfaceMonitor.class);
  private final List<Consumer<NetworkStateChange>> listeners = new ArrayList<>();

  private ScheduledFuture<?> scheduledFuture;
  private Map<String, NetworkInterfaceState> lastInterfaces;

  private NetworkInterfaceMonitor() {
  }

  public List<InetAddress> getCurrentIpAddresses() {
    List<InetAddress> addresses = new ArrayList<>();
    getCurrentNetworkInterfaces().forEach((s, networkInterfaceState) -> addresses.addAll(networkInterfaceState.getIpAddresses()));
    return addresses;
  }

  public void addListener(Consumer<NetworkStateChange> listener) {
    listeners.add(listener);
  }

  private void checkNetworkInterfaces() {
    Map<String, NetworkInterfaceState> currentInterfaces = getCurrentNetworkInterfaces();
    compareInterfaceMaps(lastInterfaces, currentInterfaces);
    lastInterfaces = currentInterfaces;
  }

  private void compareInterfaceMaps(Map<String, NetworkInterfaceState> oldMap, Map<String, NetworkInterfaceState> newMap) {
    for (Map.Entry<String, NetworkInterfaceState> entry : newMap.entrySet()) {
      if (!oldMap.containsKey(entry.getKey())) {
        notifyListeners(new NetworkStateChange(NetworkEvent.ADDED, entry.getValue()));
      } else {
        checkInterfaceStateChanges(oldMap.get(entry.getKey()), entry.getValue());
      }
    }
    for (Map.Entry<String, NetworkInterfaceState> entry : oldMap.entrySet()) {
      if (!newMap.containsKey(entry.getKey())) {
        notifyListeners(new NetworkStateChange(NetworkEvent.REMOVED, entry.getValue()));
      }
    }
  }

  private void checkInterfaceStateChanges(NetworkInterfaceState oldInterface, NetworkInterfaceState newInterface) {
    // Check for UP/DOWN state change
    if (oldInterface.isUp() != newInterface.isUp()) {
      NetworkEvent event = newInterface.isUp() ? NetworkEvent.UP : NetworkEvent.DOWN;
      notifyListeners(new NetworkStateChange(event, newInterface));
    }
    if (newInterface.isUp()) {
      List<InetAddress> oldAddresses = oldInterface.getIpAddresses();
      List<InetAddress> newAddresses = newInterface.getIpAddresses();
      if (oldAddresses.size() != newAddresses.size()) {
        notifyListeners(new NetworkStateChange(NetworkEvent.IP_CHANGED, newInterface));
      } else {
        for (int i = 0; i < oldAddresses.size(); i++) {
          if (!Arrays.equals(oldAddresses.get(i).getAddress(), newAddresses.get(i).getAddress())) {
            notifyListeners(new NetworkStateChange(NetworkEvent.IP_CHANGED, newInterface));
          }
        }
      }
    }
  }

  private void notifyListeners(NetworkStateChange change) {
    logger.log(NETWORK_MONITOR_STATE_CHANGE, change.getNetworkInterface().getName(), change.getEvent().name());
    listeners.forEach(listener -> listener.accept(change));
  }

  private Map<String, NetworkInterfaceState> getCurrentNetworkInterfaces() {
    Map<String, NetworkInterfaceState> interfacesMap = new HashMap<>();
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();
        for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
          InetAddress inetAddress = address.getAddress();
          if (!inetAddress.isLinkLocalAddress() && !inetAddress.isLoopbackAddress()) {
            interfacesMap.put(networkInterface.getName(), new NetworkInterfaceState(networkInterface));
            break;
          }
        }
      }
    } catch (SocketException e) {
      logger.log(NETWORK_MONITOR_EXCEPTION, e);
    }
    return interfacesMap;
  }

  @Override
  public String getName() {
    return "Network Interface Monitor";
  }

  @Override
  public String getDescription() {
    return "Monitors system network interfaces and updates end points dependent on the network state";
  }

  @Override
  public void start() {
    lastInterfaces = getCurrentNetworkInterfaces();
    scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::checkNetworkInterfaces, 0, 30, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
  }
}
