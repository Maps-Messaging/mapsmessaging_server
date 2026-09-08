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

package io.mapsmessaging.state.rest.twins;

import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(
    name = "Server Twin Administration",
    description = "Read-only compatibility endpoints for clients consuming configured twin metadata."
)
@Path(URI_PATH + "/server/twin/admin")
public class TwinAdminApi extends BaseRestApi {

  private static final String RESOURCE = "server/twin";

  @GET
  @Path("/drones")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List configured drones",
      description = "Returns configured drone metadata in the envelope used by twin administration clients.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Configured drones returned",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = DroneAdminConfigurationDTO.class))
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Server twin configuration error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response listDrones() {
    try {
      hasAccess(RESOURCE);
      TwinManagerConfig config = TwinManagerConfig.getInstance();
      if (config == null) {
        return internalServerError("TwinManager configuration is not available");
      }
      Collection<DroneInfoDTO> drones = new TwinConfigurationStore(config).listDrones();
      return ok(toAdminDroneConfigurations(drones));
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  static List<DroneAdminConfigurationDTO> toAdminDroneConfigurations(
      Collection<DroneInfoDTO> drones) {
    if (drones == null) {
      return List.of();
    }
    return drones.stream()
        .filter(Objects::nonNull)
        .map(DroneAdminConfigurationDTO::new)
        .toList();
  }

  private Response mapAuthOrRethrow(WebApplicationException exception) {
    Response authResponse = exception.getResponse();
    int status = authResponse == null ? 500 : authResponse.getStatus();
    if (status == 401) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new StatusResponse("Unauthorized"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (status == 403) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity(new StatusResponse("Access denied"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    throw exception;
  }
}
