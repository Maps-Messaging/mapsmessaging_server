/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.canbus.device.CanCapabilities;
import io.mapsmessaging.network.io.InterfaceInformation;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.util.List;

public class CanbusInterfaceInfo implements InterfaceInformation {

  private final CanCapabilities capabilities;

  public CanbusInterfaceInfo(CanCapabilities capabilities){
    this.capabilities = capabilities;
  }

  @Override
  public boolean isLoopback() throws SocketException {
    return false;
  }

  @Override
  public boolean isUp() throws SocketException {
    return false;
  }

  @Override
  public boolean isLoRa() {
    return false;
  }

  @Override
  public List<InterfaceAddress> getInterfaces() {
    return List.of();
  }

  @Override
  public int getMTU() throws SocketException {
    return capabilities.interfaceMaxPayloadBytes();
  }

  @Override
  public boolean isIPV4() {
    return false;
  }

  @Override
  public InetAddress getBroadcast() {
    return null;
  }
}
