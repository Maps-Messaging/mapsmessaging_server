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
 */

package io.mapsmessaging.rest.api.impl.server;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.dto.rest.cache.CacheInfo;
import io.mapsmessaging.rest.api.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Server Config Management")
@Path(URI_PATH)
public class CacheManagementApi extends ServerBaseRestApi {

  @GET
  @Path("/server/cache")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get cache information",
      description = "Retrieves cache metadata. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Cache info returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Unexpected server error")
  public CacheInfo getCacheInformation() {
    hasAccess(RESOURCE);
    return Constants.getCentralCache().getCacheInfo();
  }

  @PUT
  @Path("/server/cache")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Clear cache",
      description = "Clears the entire cache. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "204", description = "Cache cleared")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Unexpected server error")
  public void clearCacheInformation() {
    hasAccess(RESOURCE);
    Constants.getCentralCache().clear();
    response.setStatus(Response.Status.NO_CONTENT.getStatusCode());
  }
}
