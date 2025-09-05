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

package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "Server Health")
@Path("/health")
public class ConsulHealth extends BaseRestApi {

  @GET
  @Produces({MediaType.TEXT_PLAIN})
  @Operation(
      summary = "Check server health",
      description = "Checks the health of all subsystems and returns their overall status. Possible values are 'Ok', 'Warning', or 'Error'.",
      security = {}, // Overrides global security
      responses = {
          @ApiResponse(responseCode = "200", description = "Health status returned"),
          @ApiResponse(responseCode = "400", description = "Bad request")
      }
  )
  public String getHealth() {
    String state = "";
    for (SubSystemStatusDTO status : MessageDaemon.getInstance().getSubSystemManager().getSubSystemStatus()) {
      switch (status.getStatus()) {
        case ERROR:
          state = "Error";
          break;

        case WARN:
          if (state.isEmpty()) {
            state = "Warning";
          }
          break;

        default:
          break;
      }
    }
    if (state.isEmpty()) {
      state = "Ok";
    }
    return state;
  }
}
