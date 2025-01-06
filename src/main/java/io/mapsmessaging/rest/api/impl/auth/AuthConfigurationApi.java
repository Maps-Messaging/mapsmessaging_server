/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.rest.api.impl.auth;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.dto.rest.config.AuthManagerConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH)
public class AuthConfigurationApi extends BaseAuthRestApi {

  @GET
  @Path("/auth/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get the auth configuration",
      description = "Retrieves the configuration used to setup the authentication and authorisation. Requires authentication if enabled in the configuration."
  )
  public AuthManagerConfigDTO getAuthConfiguration() {
    hasAccess(RESOURCE);
    return AuthManager.getInstance().getConfig();
  }

  @POST
  @Path("/auth/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update the auth configuration",
      description = "Updates the configuration used to setup the authentication and authorisation. Requires authentication if enabled in the configuration.",
      security = {@SecurityRequirement(name ="basicAuth")}
  )
  public boolean updateAuthConfiguration(AuthManagerConfigDTO update) throws IOException {
    hasAccess(RESOURCE);
    if (AuthManager.getInstance().getConfig().update(update)) {
      AuthManager.getInstance().getConfig().save();
      return true;
    }
    return false;
  }
}
