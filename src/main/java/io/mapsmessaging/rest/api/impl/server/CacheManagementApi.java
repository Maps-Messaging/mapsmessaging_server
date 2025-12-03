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

package io.mapsmessaging.rest.api.impl.server;

import io.mapsmessaging.dto.rest.cache.CacheInfo;
import io.mapsmessaging.rest.api.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Config Management")
@Path(URI_PATH+"/server/cache")
public class CacheManagementApi extends ServerBaseRestApi {
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve cache information",
      description = "Fetches detailed information about the server's central cache, including size, usage statistics, and entries.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = CacheInfo.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public CacheInfo getCacheInformation() {
    hasAccess(RESOURCE);
    return Constants.getCentralCache().getCacheInfo();
  }

  @DELETE
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Clear cache",
      description = "Clears all entries in the server's central cache to free up memory and ensure data consistency.",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "Cache cleared successfully (no content)"
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public void clearCacheInformation() {
    hasAccess(RESOURCE);
    Constants.getCentralCache().clear();
    response.setStatus(Response.Status.NO_CONTENT.getStatusCode());
  }
}
