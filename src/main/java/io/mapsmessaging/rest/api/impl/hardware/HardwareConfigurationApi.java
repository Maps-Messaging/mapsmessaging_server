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

package io.mapsmessaging.rest.api.impl.hardware;

import io.mapsmessaging.config.DeviceManagerConfig;
import io.mapsmessaging.dto.rest.config.DeviceManagerConfigDTO;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Hardware Management")
@Path(URI_PATH+"/server/hardware/config")
public class HardwareConfigurationApi extends HardwareBaseRestApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get hardware configuration",
      description = "Retrieve the configuration for the hardware sub-system. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get hardware config was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceManagerConfigDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public DeviceManagerConfigDTO getDeviceConfig() {
    hasAccess(RESOURCE);
    return DeviceManagerConfig.getInstance();
  }

  @POST
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update hardware configuration",
      description = "Update the configuration for the hardware sub-system. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Update device config was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "304", description = "No changes made"),
      }
  )
  public StatusResponse updateDeviceConfig(DeviceManagerConfigDTO update) throws IOException {
    hasAccess(RESOURCE);
    if (DeviceManagerConfig.getInstance().update(update)) {
      DeviceManagerConfig.getInstance().save();
      return new StatusResponse("Successfully updated");
    }
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    return new StatusResponse("Failed to update");
  }
}
