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

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.mavlink.MavlinkEventFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Optional;

public class MavlinkSerialProtocol extends MavlinkProtocol {
  private static final MavlinkDeviceKey DUMMY_KEY = new MavlinkDeviceKey(0, InetSocketAddress.createUnresolved("localhost", 0), 0);

  private final SelectorTask selectorTask;
  private final MavlinkEventFactory mavlinkEventFactory;

  protected MavlinkSerialProtocol(@NonNull @NotNull EndPoint endPoint,
                                  @NotNull @NonNull ProtocolConfigDTO protocolConfig) throws IOException {
    super(key1 -> {}, DUMMY_KEY, endPoint, protocolConfig);
    mavlinkEventFactory  = new MavlinkEventFactory();
    if(endPoint instanceof SerialEndPoint serialEndPoint){
      serialEndPoint.setStreamHandler(new MavlinkStreamHandler());
    }
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
  }


  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    byte[] raw = new byte[packet.available()];
    packet.get(raw);
    Optional<ProcessedFrame> potentialFrame = mavlinkEventFactory.unpack(endPoint.getName(), ByteBuffer.wrap(raw));
    if(potentialFrame.isPresent()) {
      ProcessedFrame env = potentialFrame.get();
      processRawFrame(env, raw);
    }
    packet.clear();
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    return true;
  }
}
