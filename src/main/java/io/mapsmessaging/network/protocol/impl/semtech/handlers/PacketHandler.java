/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.*;

import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import java.util.Arrays;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class PacketHandler {

  private static final PacketHandler instance = new PacketHandler();

  public static PacketHandler getInstance() {
    return instance;
  }

  private final Handler[] handlers;

  @Getter
  private final MessageStateContext messageStateContext;

  private PacketHandler() {
    messageStateContext = new MessageStateContext();
    handlers = new Handler[MAX_EVENTS];
    Arrays.fill(handlers, null);
    handlers[PUSH_DATA] = new PushDataHandler();
    handlers[PULL_DATA] = new PullDataHandler();
    handlers[TX_ACK] = new TxAckHandler();
  }

  public void handle(@NotNull @NonNull SemTechProtocol protocol, @NotNull @NonNull SemTechPacket packet) {
    Handler handler = handlers[packet.getIdentifier()];
    if (handler != null) {
      handler.process(protocol, packet);
    }
  }
}
