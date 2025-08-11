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

package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayManager;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import io.mapsmessaging.network.protocol.impl.semtech.packet.TxAcknowledge;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

public class TxAckHandler extends Handler {

  @Override
  public void process(@NotNull @NonNull SemTechProtocol protocol, @NotNull @NonNull SemTechPacket packet) {
    TxAcknowledge txAck = (TxAcknowledge) packet;
    PacketHandler.getInstance().getMessageStateContext().complete(txAck.getToken());
    GatewayInfo info = protocol.getGatewayManager().getInfo(GatewayManager.dumpIdentifier(txAck.getGatewayIdentifier()));
    if (info != null) {
      sendMessage(protocol, info, packet.getFromAddress());
    }
    if (!txAck.getJsonObject().isEmpty()) {
      Map<String, String> meta = new LinkedHashMap<>();
      meta.put("protocol", "SemTech");
      meta.put("version", "" + VERSION);
      meta.put("time_ms", "" + System.currentTimeMillis());
      MessageBuilder builder = new MessageBuilder();
      builder.setOpaqueData(txAck.getJsonObject().getBytes(StandardCharsets.UTF_8));
      builder.setMeta(meta);
      Message message = MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), builder).build();
      try {
        if (info != null) {
          info.getInbound().storeMessage(message);
        }
      } catch (IOException e) {
        // Catch & ignore
      }
    }
  }
}
