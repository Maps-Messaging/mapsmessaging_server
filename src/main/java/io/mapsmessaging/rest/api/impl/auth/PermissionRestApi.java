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

package io.mapsmessaging.rest.api.impl.auth;


import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.dto.rest.config.auth.AuthorisationConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.PermissionDetailsDTO;
import io.mapsmessaging.dto.rest.config.auth.ResourceTypeDetailsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH)
public class PermissionRestApi extends BaseAuthRestApi {

  @GET
  @Path("/auth/permissions")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get the authorisation permission list",
      description = "Retrieves the read only permissions used by the servers Authorisation",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get permissions was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthorisationConfigDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),

      }
  )
  public AuthorisationConfigDTO getAuthorisationStaticInfo() {
    hasAccess(RESOURCE);
    List<PermissionDetailsDTO> details = new ArrayList<>();
    for(ServerPermissions permission : ServerPermissions.values()){
      details.add(new PermissionDetailsDTO(permission));
    }
    List<ResourceTypeDetailsDTO> resourceTypes = new ArrayList<>();
    resourceTypes.add(new ResourceTypeDetailsDTO("Server", true));
    for(DestinationType type : DestinationType.values()){
      resourceTypes.add(new ResourceTypeDetailsDTO(type.getName(), false));
    }

    return new AuthorisationConfigDTO(details,resourceTypes);
  }
}
