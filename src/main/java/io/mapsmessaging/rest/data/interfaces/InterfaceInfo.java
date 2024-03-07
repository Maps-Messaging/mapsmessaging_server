/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.data.interfaces;

import io.mapsmessaging.network.EndPointManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

@ToString
@Data
public class InterfaceInfo implements Serializable {

  @Schema(description = "Unique name of the interface")
  private final String name;
  @Schema(description = "Port that the interface is bound to")
  private final int port;
  @Schema(description = "Host that the interface is bound to")
  private final String host;
  @Schema(description = "Current state of the interface")
  private final String state;
  @Schema(description = "Configuration for the interface")
  private final Map<String, Object> config;

  public InterfaceInfo(EndPointManager endPointManager){
    name = (endPointManager.getEndPointServer().getConfig().getProperties().getProperty("name"));
    port = (endPointManager.getEndPointServer().getUrl().getPort());
    host = (endPointManager.getEndPointServer().getUrl().getHost());
    config = (endPointManager.getEndPointServer().getConfig().getProperties().getMap());
    switch (endPointManager.getState()) {
      case START:
        state = ("Started");
        break;
      case STOPPED:
        state = ("Stopped");
        break;
      case PAUSED:
        state = ("Paused");
        break;

      default:
        state = ("Unknown");
    }

  }

}
