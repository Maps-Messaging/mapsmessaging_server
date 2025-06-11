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

package io.mapsmessaging.network.monitor;

import io.mapsmessaging.config.NetworkManagerConfig;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;

import java.net.*;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class NetworkInterfaceMonitor implements Agent {

  private static final String IPv4_ALL_HOSTS = "0.0.0.0";
  private static final String IPv6_ALL_HOSTS = "::";

  @Getter
  private static final NetworkInterfaceMonitor instance = new NetworkInterfaceMonitor();

  private final Logger logger = LoggerFactory.getLogger(NetworkInterfaceMonitor.class);
  private final List<Consumer<NetworkStateChange>> listeners = new ArrayList<>();

  private final boolean enabled;
  private final long interval;

  private ScheduledFuture<?> scheduledFuture;
  private Map<String, NetworkInterfaceState> lastInterfaces;

  private NetworkInterfaceMonitor() {
    NetworkManagerConfig networkManagerConfig = NetworkManagerConfig.getInstance();
    enabled = networkManagerConfig.isScanNetworkChanges();
    interval = networkManagerConfig.getScanInterval();
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
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();
        if (networkInterface.isUp()) {
          logger.log(NETWORK_MONITOR_DISCOVERED_DEVICES, networkInterface.getName(), networkInterface.getDisplayName());
        }
      }
    } catch (SocketException e) {
      // Ignore
    }
    if (enabled) {
      scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::checkNetworkInterfaces, interval, interval, TimeUnit.SECONDS);
    }
  }

  @Override
  public void stop() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
  }

  public boolean ipAddressMatches(String source, InetAddress test) {
    return (source.equals(test.getHostAddress()) ||
        test instanceof Inet4Address && source.equals(IPv4_ALL_HOSTS) ||
        test instanceof Inet6Address && source.equals(IPv6_ALL_HOSTS));
  }

  public List<InetAddress> getIpAddressByName(String adapterName) {
    List<InetAddress> list = new ArrayList<>();
    getCurrentNetworkInterfaces().forEach((s, networkInterfaceState) -> {
      if (networkInterfaceState.getName().equals(adapterName)) {
        list.addAll(networkInterfaceState.getIpAddresses());
      } else if (adapterName.equals(IPv4_ALL_HOSTS)) {
        networkInterfaceState.getIpAddresses().forEach((inetAddress) -> {
          if (inetAddress instanceof Inet4Address) {
            list.add(inetAddress);
          }
        });
      } else if (adapterName.equals(IPv6_ALL_HOSTS)) {
        list.addAll(networkInterfaceState.getIpAddresses());
      }
    });
    if (list.isEmpty()) {
      try {
        InetAddress inetAddress = InetAddress.getByName(adapterName);
        if (inetAddress != null) {
          list.add(inetAddress);
          logger.log(NETWORK_MONITOR_RESOLVE_SUCCESS, adapterName, inetAddress.getHostAddress());
        } else {
          logger.log(NETWORK_MONITOR_RESOLVE_ERROR, adapterName);
        }
      } catch (UnknownHostException e) {
        logger.log(NETWORK_MONITOR_RESOLVE_ERROR, adapterName, e);
      }
    }
    return list;
  }

  public List<InetAddress> getCurrentIpAddresses() {
    List<InetAddress> addresses = new ArrayList<>();
    getCurrentNetworkInterfaces().forEach((s, networkInterfaceState) -> addresses.addAll(networkInterfaceState.getIpAddresses()));
    return addresses;
  }

  public void addListener(Consumer<NetworkStateChange> listener) {
    listeners.add(listener);
  }

  public void removeListener(Consumer<NetworkStateChange> listener) {
    listeners.remove(listener);
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
        notifyListeners(new NetworkStateChange(NetworkEvent.DOWN, oldInterface));
        notifyListeners(new NetworkStateChange(NetworkEvent.IP_CHANGED, newInterface));
      } else {
        for (int i = 0; i < oldAddresses.size(); i++) {
          if (!Arrays.equals(oldAddresses.get(i).getAddress(), newAddresses.get(i).getAddress())) {
            notifyListeners(new NetworkStateChange(NetworkEvent.DOWN, oldInterface));
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
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("");
    if(enabled){
      status.setStatus(Status.OK);
      status.setComment("Monitoring : "+lastInterfaces.size());
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
  }

}
