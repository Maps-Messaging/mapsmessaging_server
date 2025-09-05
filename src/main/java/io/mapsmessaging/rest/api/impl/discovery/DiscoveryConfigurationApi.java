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

package io.mapsmessaging.rest.api.impl.discovery;

import io.mapsmessaging.config.DiscoveryManagerConfig;
import io.mapsmessaging.dto.rest.config.DiscoveryManagerConfigDTO;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Discovery Management")
@Path(URI_PATH)
public class DiscoveryConfigurationApi extends DiscoveryBaseRestApi {


  @GET
  @Path("/server/discovery/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get the discovery agents configuration",
      description = "Retrieves the configuration used to the discovery agent. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get discobvery config was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiscoveryManagerConfigDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public DiscoveryManagerConfigDTO getDiscoveryAgentConfiguration() {
    hasAccess(RESOURCE);
    return DiscoveryManagerConfig.getInstance();
  }

  @POST
  @Path("/server/discovery/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update the discovery agents configuration",
      description = "Updates the configuration used to control the discovery agent. Requires authentication if enabled in the configuration.",
      security = {@SecurityRequirement(name = "basicAuth")},
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Update discovery configuration was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "304", description = "No changes made"),
      }
  )
  public StatusResponse updateDiscoveryAgentConfiguration(DiscoveryManagerConfigDTO update) throws IOException {
    hasAccess(RESOURCE);
    if (DiscoveryManagerConfig.getInstance().update(update)) {
      DiscoveryManagerConfig.getInstance().save();
      return new StatusResponse("Successfully updated the discovery agent configuration");
    }
    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    return new StatusResponse("Failed to update the discovery agent configuration");
  }

}
