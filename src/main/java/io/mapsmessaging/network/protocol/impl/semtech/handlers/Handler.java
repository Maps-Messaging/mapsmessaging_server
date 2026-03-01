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

package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechTransmitDefaultsDTO;
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
    if (messageEvent == null) {
      return;
    }

    Message raw = messageEvent.getMessage();
    if (protocol.getProtocolMessageTransformation() != null) {
      raw = protocol.getProtocolMessageTransformation().outgoing(messageEvent.getMessage(), messageEvent.getDestinationName());
    }

    try {
      byte[] pullRespJson = buildPullResponseJson(raw.getOpaqueData(), info.getTransmitDefaults());

      int token = nextToken();
      PacketHandler.getInstance().getMessageStateContext().push(token, messageEvent);

      PullResponse pullResponse = new PullResponse(token, pullRespJson, socketAddress);
      protocol.sendPacket(pullResponse);
      protocol.getLogger().log(ServerLogMessages.SEMTECH_SENDING_PACKET, messageEvent.getMessage());
    } catch (Exception e) {
      messageEvent.getCompletionTask().run();
    }
  }

  private byte[] buildPullResponseJson(byte[] opaqueData, SemtechTransmitDefaultsDTO defaults) {
    if (opaqueData == null) {
      throw new IllegalArgumentException("Opaque data must not be null");
    }

    JsonObject maybeJson = tryParseJsonObject(opaqueData);
    if (maybeJson != null && maybeJson.has("txpk") && maybeJson.get("txpk").isJsonObject()) {
      JsonObject txpk = maybeJson.getAsJsonObject("txpk");
      normalizeAndValidateTxpk(txpk, defaults);

      JsonObject wrapper = new JsonObject();
      wrapper.add("txpk", txpk);
      return wrapper.toString().getBytes(StandardCharsets.UTF_8);
    }

    return buildTxpkFromRawPayload(opaqueData, defaults);
  }

  private JsonObject tryParseJsonObject(byte[] data) {
    try {
      String text = new String(data, StandardCharsets.UTF_8).trim();
      if (text.isEmpty()) {
        return null;
      }

      JsonElement element = JsonParser.parseString(text);
      if (!element.isJsonObject()) {
        return null;
      }
      return element.getAsJsonObject();
    } catch (Exception e) {
      return null;
    }
  }

  private void normalizeAndValidateTxpk(JsonObject txpk, SemtechTransmitDefaultsDTO defaults) {
    if (txpk == null) {
      throw new IllegalArgumentException("txpk must not be null");
    }

    if (!txpk.has("data") || txpk.get("data").isJsonNull()) {
      throw new IllegalArgumentException("txpk.data is required");
    }

    String base64 = txpk.get("data").getAsString();
    if (base64 == null || base64.isBlank()) {
      throw new IllegalArgumentException("txpk.data must not be blank");
    }

    byte[] decoded;
    try {
      decoded = java.util.Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("txpk.data is not valid base64", e);
    }

    if (txpk.has("size") && !txpk.get("size").isJsonNull()) {
      int declaredSize = txpk.get("size").getAsInt();
      if (declaredSize != decoded.length) {
        throw new IllegalArgumentException("txpk.size does not match decoded base64 length");
      }
    } else {
      txpk.addProperty("size", decoded.length);
    }

    if (defaults == null) {
      return;
    }
    putIfMissing(txpk, "imme", defaults.isImme());
    putIfMissing(txpk, "freq", defaults.getFreq());
    putIfMissing(txpk, "rfch", defaults.getRfch());
    putIfMissing(txpk, "powe", defaults.getPowe());
    putIfMissing(txpk, "modu", defaults.getModu());
    putIfMissing(txpk, "datr", defaults.getDatr());
    putIfMissing(txpk, "codr", defaults.getCodr());
    putIfMissing(txpk, "ipol", defaults.isIpol());
  }

  private byte[] buildTxpkFromRawPayload(byte[] payload, SemtechTransmitDefaultsDTO defaults) {
    if (defaults == null) {
      throw new IllegalArgumentException("Transmit defaults are required to build txpk from raw payload");
    }

    if (payload.length == 0) {
      throw new IllegalArgumentException("Payload must not be empty");
    }

    String base64 = java.util.Base64.getEncoder().encodeToString(payload);

    JsonObject txpk = new JsonObject();
    txpk.addProperty("imme", defaults.isImme());
    txpk.addProperty("freq", defaults.getFreq());
    txpk.addProperty("rfch", defaults.getRfch());
    txpk.addProperty("powe", defaults.getPowe());
    txpk.addProperty("modu", defaults.getModu());
    txpk.addProperty("datr", defaults.getDatr());
    txpk.addProperty("codr", defaults.getCodr());
    txpk.addProperty("ipol", defaults.isIpol());
    txpk.addProperty("size", payload.length);
    txpk.addProperty("data", base64);

    JsonObject wrapper = new JsonObject();
    wrapper.add("txpk", txpk);

    return wrapper.toString().getBytes(StandardCharsets.UTF_8);
  }

  private void putIfMissing(JsonObject object, String key, boolean value) {
    if (!object.has(key) || object.get(key).isJsonNull()) {
      object.addProperty(key, value);
    }
  }

  private void putIfMissing(JsonObject object, String key, int value) {
    if (!object.has(key) || object.get(key).isJsonNull()) {
      object.addProperty(key, value);
    }
  }

  private void putIfMissing(JsonObject object, String key, double value) {
    if (!object.has(key) || object.get(key).isJsonNull()) {
      object.addProperty(key, value);
    }
  }

  private void putIfMissing(JsonObject object, String key, String value) {
    if (value == null) {
      return;
    }
    if (!object.has(key) || object.get(key).isJsonNull()) {
      object.addProperty(key, value);
    }
  }

  private static int nextToken() {
    return (TokenGenerator.nextInt() % 0xffff);
  }
}
