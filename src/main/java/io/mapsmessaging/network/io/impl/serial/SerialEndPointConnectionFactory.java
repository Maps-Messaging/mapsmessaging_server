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

package io.mapsmessaging.network.io.impl.serial;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortScanner;

import java.io.IOException;
import java.util.List;

public class SerialEndPointConnectionFactory implements EndPointConnectionFactory {

  // We need to open a socket, it's a socket library so we can ignore this issue
  @java.lang.SuppressWarnings({"squid:S4818", "squid:S2095"})
  @Override
  public EndPoint connect(EndPointURL url, SelectorLoadManager selector, EndPointConnectedCallback connectedCallback, EndPointServerStatus endPointServerStatus,
                          List<String> jmxPath) throws IOException {

    SerialConfigDTO serialConfig = (SerialConfigDTO) endPointServerStatus.getConfig().getEndPointConfig();
    SerialPort serialPort = getSerialPort(serialConfig);
    if(serialPort == null) {
      throw new IOException("Serial device not yet connected");
    }
    SerialEndPoint endPoint = new SerialEndPoint(generateID(), endPointServerStatus, serialPort, serialConfig,  jmxPath );
    connectedCallback.connected(endPoint);
    return endPoint;
  }

  public SerialPort getSerialPort(SerialConfigDTO serialConfig) {
    String serialNo = serialConfig.getSerialNo();
    String port = serialConfig.getPort();

    if (serialNo != null && !serialNo.isEmpty()) {
      return SerialPortScanner.getInstance().allocatePortBySerial(serialNo);
    } else if (port != null && !port.isEmpty()) {
      return SerialPortScanner.getInstance().allocatePort(port);
    }
    return null;
  }


  @Override
  public String getName() {
    return "serial";
  }

  @Override
  public String getDescription() {
    return "serial connection end point factory";
  }

}
