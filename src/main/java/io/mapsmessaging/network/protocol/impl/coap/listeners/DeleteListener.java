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

package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;

public class DeleteListener extends PublishListener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) {

    super.publishMessage(request, protocol, true);
    return null;
  }

  @Override
  protected Message build(BasePacket request, CoapProtocol protocol){
    MessageBuilder messageBuilder = new MessageBuilder(); // this simply deletes the entry
    messageBuilder.setOpaqueData(null);
    messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
    messageBuilder.setRetain(true);
    return messageBuilder.build();
  }
}