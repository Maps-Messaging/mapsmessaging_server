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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.mavlink.message.Frame;

import java.util.Base64;
import java.util.Map;

public final class MavlinkJsonEnvelopeBuilder {

  private static final Gson GSON = GsonFactory.createStrictJsonWithSafeFloats();

  public static JsonObject toJson(
      Frame envelope,
      Map<String, Object> decodedPayload
  ) {

    JsonObject root = new JsonObject();

    root.addProperty("version", envelope.getVersion().name());
    root.addProperty("messageId", envelope.getMessageId());
    root.addProperty("systemId", envelope.getSystemId());
    root.addProperty("componentId", envelope.getComponentId());
    root.addProperty("sequence", envelope.getSequence());
    root.addProperty("payloadLength", envelope.getPayloadLength());
    root.addProperty("signed", envelope.isSigned());

    JsonObject payload = new JsonObject();

    payload.addProperty("rawBase64", Base64.getEncoder().encodeToString(envelope.getPayload()));

    JsonElement humanReadable = GSON.toJsonTree(decodedPayload);
    payload.add("decoded", humanReadable);

    root.add("payload", payload);

    return root;
  }
}