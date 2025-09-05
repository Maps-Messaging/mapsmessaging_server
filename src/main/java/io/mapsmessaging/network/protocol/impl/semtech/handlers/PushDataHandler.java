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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PushAck;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PushData;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

public class PushDataHandler extends Handler {

  @Override
  public void process(@NotNull @NonNull SemTechProtocol protocol, @NotNull @NonNull SemTechPacket packet) {
    PushData pushData = (PushData) packet;
    if (pushData.isValid()) {
      protocol.sendPacket(new PushAck(pushData.getToken(), packet.getFromAddress()));
      if (pushData.getJsonObject() != null && !pushData.getJsonObject().isEmpty()) {
        try {
          JsonObject jsonObject = JsonParser.parseString(pushData.getJsonObject()).getAsJsonObject();
          boolean status = jsonObject.has("stat");

          Map<String, String> meta = new LinkedHashMap<>();
          meta.put("protocol", "SemTech");
          meta.put("version", String.valueOf(VERSION));
          meta.put("sessionId", protocol.getSessionId());
          meta.put("time_ms", "" + System.currentTimeMillis());
          MessageBuilder builder = new MessageBuilder();
          builder.setOpaqueData(pushData.getJsonObject().getBytes(StandardCharsets.UTF_8));
          builder.setMeta(meta);
          Message message = MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), builder).build();

          GatewayInfo info = protocol.getGatewayManager().getInfo(pushData.getGatewayIdentifier());
          if (info != null) {
            if (status) {
              info.getStatus().storeMessage(message);
            } else {
              info.getInbound().storeMessage(message);
            }
          }
        } catch (JsonParseException | IOException e) {

        }
      }
    }
  }
}
