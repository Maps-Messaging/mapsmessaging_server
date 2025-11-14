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
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullResponse;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;

public abstract class Handler {

  private static final Random TokenGenerator = new SecureRandom();

  abstract void process(@NotNull @NonNull SemTechProtocol protocol, @NotNull @NonNull SemTechPacket packet);

  public void sendMessage(SemTechProtocol protocol, GatewayInfo info, SocketAddress socketAddress) {
    MessageEvent messageEvent = info.getWaitingMessages().poll();
    if (messageEvent != null) {
      Message raw = messageEvent.getMessage();
      if (protocol.getProtocolMessageTransformation() != null) {
        raw = protocol.getProtocolMessageTransformation().outgoing(messageEvent.getMessage(), messageEvent.getDestinationName());
      }
      try {
        JsonObject jsonObject = JsonParser.parseString(new String(raw.getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();
        int token = nextToken();
        PacketHandler.getInstance().getMessageStateContext().push(token, messageEvent);
        PullResponse pullResponse = new PullResponse(token, raw.getOpaqueData(), socketAddress);
        protocol.sendPacket(pullResponse);
        protocol.getLogger().log(ServerLogMessages.SEMTECH_SENDING_PACKET, messageEvent.getMessage());
      } catch (JsonParseException e) {
        messageEvent.getCompletionTask().run();
      }

    }
  }

  private static int nextToken() {
    return (TokenGenerator.nextInt() % 0xffff);
  }
}
