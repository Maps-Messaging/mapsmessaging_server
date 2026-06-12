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

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.config.protocol.impl.NmeaConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class NMEAInterfaceManager implements SelectorCallback {


  private static final Logger logger = LoggerFactory.getLogger(NMEAInterfaceManager.class);
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final NmeaConfig nmeaConfig;

  public NMEAInterfaceManager(EndPoint endPoint) throws IOException {
    this.endPoint = endPoint;
    nmeaConfig = (NmeaConfig) endPoint.getConfig().getProtocolConfig("NMEA-0183");

    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    // OK, we have received a packet, lets find out if we have an existing context for it
    if (packet.getFromAddress() == null) {
      return true; // Ignoring packet since unknown client
    }
    byte[] raw = new byte[packet.available()];
    int pos = packet.position();
    packet.get(raw);
    packet.position(pos);

    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }


  @Override
  public void close() {

  }

  @Override
  public String getName() {
    return "NMEA-0183";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }
}
