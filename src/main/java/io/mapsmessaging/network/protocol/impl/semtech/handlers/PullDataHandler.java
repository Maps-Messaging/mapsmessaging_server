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

import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayManager;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullAck;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullData;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class PullDataHandler extends Handler {

  @Override
  public void process(@NotNull @NonNull SemTechProtocol protocol, @NotNull @NonNull SemTechPacket packet) {
    PullData pullData = (PullData) packet;
    protocol.sendPacket(new PullAck(pullData.getToken(), packet.getFromAddress()));
    GatewayInfo info = protocol.getGatewayManager().getInfo(GatewayManager.dumpIdentifier(pullData.getGatewayIdentifier()));
    if (info != null) {
      sendMessage(protocol, info, packet.getFromAddress());
    }
  }

}
