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

package io.mapsmessaging.dto.helpers;

import io.mapsmessaging.dto.rest.interfaces.InterfaceInfoDTO;
import io.mapsmessaging.network.EndPointManager;

public class InterfaceInfoHelper {
  public static InterfaceInfoDTO fromEndPointManager(EndPointManager endPointManager) {
    InterfaceInfoDTO dto = new InterfaceInfoDTO();
    dto.setUniqueId(endPointManager.getUniqueId().toString());
    dto.setName(endPointManager.getEndPointServer().getConfig().getName());
    dto.setPort(endPointManager.getEndPointServer().getUrl().getPort());
    dto.setHost(
        endPointManager.getEndPointServer().getUrl().getProtocol()
            + "://"
            + endPointManager.getEndPointServer().getUrl().getHost()
    );
    dto.setConfig(endPointManager.getEndPointServer().getConfig());

    // Set state based on the EndPointManager's state
    switch (endPointManager.getState()) {
      case START:
        dto.setState("Started");
        break;
      case STOPPED:
        dto.setState("Stopped");
        break;
      case PAUSED:
        dto.setState("Paused");
        break;
      default:
        dto.setState("Unknown");
    }
    return dto;
  }

  private InterfaceInfoHelper() {
  }
}
