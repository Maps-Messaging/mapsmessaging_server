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

package io.mapsmessaging.network.protocol.impl.tak;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.NoOpDetection;
import io.mapsmessaging.network.protocol.impl.extension.ExtensionEndPoint;
import io.mapsmessaging.network.protocol.impl.extension.ExtensionProtocol;

import java.io.IOException;

public class TakProtocolFactory extends ProtocolImplFactory {

  public TakProtocolFactory() {
    super("tak", "TAK Cursor-on-Target extension protocol", new NoOpDetection());
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    ExtensionConfigDTO extensionConfig;
    if (endPoint instanceof ExtensionEndPoint extensionEndPoint && extensionEndPoint.config() instanceof ExtensionConfigDTO config) {
      extensionConfig = config;
    } else {
      extensionConfig = null;
      if (endPoint.getConfig() != null && endPoint.getConfig().getProtocolConfigs() != null) {
        for (ProtocolConfigDTO protocolConfig : endPoint.getConfig().getProtocolConfigs()) {
          if (protocolConfig instanceof ExtensionConfigDTO config && getName().equalsIgnoreCase(config.getProtocol())) {
            extensionConfig = config;
            break;
          }
        }
      }
      if (extensionConfig == null) {
        throw new IOException("TAK extension configuration not found");
      }
    }

    Protocol protocol = new ExtensionProtocol(endPoint, new TakExtension(endPoint, extensionConfig));
    protocol.connect(sessionId, username, password);
    return protocol;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) {
    // TAK extension does not accept inbound client sockets.
  }

  @Override
  public String getTransportType() {
    return "tak";
  }
}
