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

package io.mapsmessaging.hardware.device;

import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import lombok.Getter;

import java.security.Principal;

@Getter
public class DeviceClientConnection implements ClientConnection {

  private final DeviceHandler deviceHandler;

  public DeviceClientConnection(DeviceHandler deviceHandler){
    this.deviceHandler = deviceHandler;
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  @Override
  public String getName() {
    return deviceHandler.getBusName();
  }

  @Override
  public String getVersion() {
    return deviceHandler.getVersion();
  }

  @Override
  public void sendKeepAlive() {
    // No keep alive required
  }

  @Override
  public Principal getPrincipal() {
    return deviceHandler::getName;
  }

  @Override
  public String getAuthenticationConfig() {
    return "";
  }

  @Override
  public String getUniqueName() {
    return deviceHandler.getName();
  }

  @Override
  public String getProtocolName() {
    return "";
  }

  @Override
  public String getRemoteIp() {
    return "";
  }
}
