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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.io;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import lombok.Getter;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;

public class SatelliteEndPoint extends EndPoint {

  @Getter
  private RemoteDeviceInfo terminalInfo;

  protected SatelliteEndPoint(long id, EndPointServerStatus server, RemoteDeviceInfo terminal) {
    super(id, server);
    this.terminalInfo = terminal;
  }

  public void sendMessage(MessageData submitMessage) {
    ((SatelliteEndPointServer) server).sendClientMessage(terminalInfo.getUniqueId(), submitMessage);
  }

  public void updateTerminalInfo() throws IOException, InterruptedException {
    RemoteDeviceInfo info = ((SatelliteEndPointServer) server).updateTerminalInfo(terminalInfo.getUniqueId());
    if(info != null && info.getUniqueId().equals(terminalInfo.getUniqueId())) {
      terminalInfo = info;
    }
  }

  public void mute(){
    ((SatelliteEndPointServer) server).mute(terminalInfo.getUniqueId());
  }

  public void unmute(){
    ((SatelliteEndPointServer) server).unmute(terminalInfo.getUniqueId());
  }

  @Override
  public String getProtocol() {
    return "satellite";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    return 0;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    return 0;
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return null;
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(getClass());
  }

}
