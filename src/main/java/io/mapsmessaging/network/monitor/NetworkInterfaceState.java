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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode
@ToString
public class NetworkInterfaceState {

  private final String name;
  private final String displayName;
  private final boolean isUp;
  private final List<InetAddress> ipAddresses;

  public NetworkInterfaceState(NetworkInterface networkInterface) throws SocketException {
    this.name = networkInterface.getName();
    this.isUp = networkInterface.isUp();
    this.ipAddresses = Collections.list(networkInterface.getInetAddresses());
    this.displayName = networkInterface.getDisplayName();
  }
}
