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

package io.mapsmessaging.network.protocol.impl.mavlink;

import lombok.Data;
import lombok.ToString;

import java.net.InetSocketAddress;

@Data
@ToString
public class MavlinkDeviceKey {
  private final int localPort;
  private final InetSocketAddress remoteAddress;
  private final int remotePort;
  private final int systemId;
  private final int hash;

  public MavlinkDeviceKey(int localPort,
                          InetSocketAddress remoteAddress,
                          int systemId
                          ){
    this.localPort = localPort;
    this.remoteAddress = remoteAddress;
    this.remotePort = remoteAddress.getPort();
    this.systemId = systemId;
    this.hash = computeHash();
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MavlinkDeviceKey other)) return false;
    return localPort == other.localPort
        && remotePort == other.remotePort
        && systemId == other.systemId
        && remoteAddress.equals(other.remoteAddress);
  }

  private int computeHash() {
    int h = 17;
    h = 31 * h + localPort;
    h = 31 * h + remoteAddress.hashCode();
    h = 31 * h + systemId;
    return h;
  }
}
