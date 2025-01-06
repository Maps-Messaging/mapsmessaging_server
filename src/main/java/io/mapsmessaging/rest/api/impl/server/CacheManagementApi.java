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

package io.mapsmessaging.rest.api.impl.server;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.dto.rest.cache.CacheInfo;
import io.mapsmessaging.rest.api.Constants;
import io.swagger.v3.oas.annotations.Operation;
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
      summary = "Retrieve cache information",
      description = "Fetches detailed information about the server's central cache, including size, usage statistics, and entries."
  )
  public CacheInfo getCacheInformation() {
    hasAccess(RESOURCE);
    return Constants.getCentralCache().getCacheInfo();
  }

  @PUT
  @Path("/server/cache")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Clear cache",
      description = "Clears all entries in the server's central cache to free up memory and ensure data consistency."
  )
  public void clearCacheInformation() {
    hasAccess(RESOURCE);
    Constants.getCentralCache().clear();
    response.setStatus(Response.Status.NO_CONTENT.getStatusCode());
  }
}
