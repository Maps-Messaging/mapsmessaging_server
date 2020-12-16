/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp.frames;

import java.util.Map;
import org.maps.messaging.api.message.TypedData;
import org.maps.network.io.Packet;

public class Message extends ServerFrame {

  private static final byte[] COMMAND = "MESSAGE".getBytes();

  private final org.maps.messaging.api.message.Message internalMessage;

  public Message() {
    internalMessage = null;
  }

  public Message(org.maps.messaging.api.message.Message message, String destination, String subscriptionId) {
    internalMessage = message;
    packMessage(destination, subscriptionId);
  }

  @Override
  public Frame instance() {
    return new Message();
  }

  byte[] getCommand() {
    return COMMAND;
  }

  private void packMessage(String destination, String subscriptionId) {
    //
    // Map the data map to the header
    //
    Map<String, TypedData> dataMap = internalMessage.getDataMap();
    if (dataMap != null) {
      for (Map.Entry<String, TypedData> entry : dataMap.entrySet()) {
        putHeader(entry.getKey(), entry.getValue().getData().toString());
      }
    }

    //
    // Map the meta data to the header
    //
    Map<String, String> metaMap = internalMessage.getMeta();
    if (metaMap != null) {
      for (Map.Entry<String, String> entry : metaMap.entrySet()) {
        putHeader(entry.getKey(), entry.getValue());
      }
    }

    //
    // Ensure the defined header messages are correct and not driven by the entries in the map
    //
    putHeader("message-id", "" + internalMessage.getIdentifier());
    putHeader("subscription", subscriptionId);
    putHeader("destination", destination);
    if (internalMessage.getOpaqueData() != null && internalMessage.getOpaqueData().length > 0) {
      putHeader("content-length", "" + internalMessage.getOpaqueData().length);
    }
    putHeader("priority", "" + internalMessage.getPriority());
  }

  public void packBody(Packet packet) {
    if (internalMessage.getOpaqueData() != null && internalMessage.getOpaqueData().length > 0) {
      packet.put(internalMessage.getOpaqueData());
    }
  }

  @Override
  public String toString() {
    return "STOMP Message[ Header:" + getHeaderAsString() + "]";
  }
}
