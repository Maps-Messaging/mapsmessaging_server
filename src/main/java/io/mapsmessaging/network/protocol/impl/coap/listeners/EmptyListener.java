/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Empty;
import java.io.IOException;

public class EmptyListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) throws IOException {
    switch (request.getType()) {
      case ACK:
        if (request.getToken() != null) {
          protocol.ack(request);
        }
        break;

      case RST:
        try {
          protocol.close();
        } catch (IOException e) {
          //
        }
        break;

      case CON:
        BasePacket pingResponse = new Empty(request.getMessageId());
        pingResponse.setFromAddress(request.getFromAddress());
        protocol.sendResponse(pingResponse);
        break;
      case NON:
        break;

    }
    return null;
  }
}
