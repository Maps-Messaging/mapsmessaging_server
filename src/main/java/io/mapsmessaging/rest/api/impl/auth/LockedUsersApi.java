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




import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.dto.rest.auth.UserDTO;
import io.mapsmessaging.security.access.monitor.LockStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;


import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH+"/auth/user-lockouts")
public class LockedUsersApi extends BaseAuthRestApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all currently locked users",
      description = "Retrieves all currently known users that are locked out due to failed log in attempts.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all users was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LockStatus[].class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public LockStatus[] getAllLockedUsers() {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    return authManager.getAllLockedUsers().toArray(new LockStatus[0]);
  }

  @DELETE
  @Path("/{username}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Unlocks a user that is currently locked due to invalid login attempts",
      description = "When a user exceeds the failed login attempts they are locked out for a period of time",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Unlock was successful",
              content = @Content(mediaType = "application/json")
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public void unlockUser(@PathParam("username") String username) {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    authManager.unlockUser(username);
  }

}
