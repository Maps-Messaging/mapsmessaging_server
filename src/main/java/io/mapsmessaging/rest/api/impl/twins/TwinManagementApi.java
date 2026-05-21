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

package io.mapsmessaging.rest.api.impl.twins;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.Optional;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Twin Management")
@Path(URI_PATH + "/server/twin")
public class TwinManagementApi extends BaseRestApi {

  private static final String RESOURCE = "server/twin";

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List twins",
      description = "Returns the list of currently known twins.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "List of twins returned",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = EntityTwin.class))
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Server twin error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response listTwins() {
    try {
      hasAccess(RESOURCE);
      Collection<EntityTwin> twins = MessageDaemon.getInstance().getSubSystemManager().getTwinManager().listTwins();
      EntityTwin[] response = twins.toArray(new EntityTwin[0]);
      return ok(response);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin error");
    }
  }

  @GET
  @Path("/{twinId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get twin",
      description = "Returns the twin for the specified twinId.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Twin returned",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = EntityTwin.class)
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Twin not found",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Server twin error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response getTwin(@PathParam("twinId") String twinId) {
    try {
      hasAccess(RESOURCE);

      if (twinId == null || twinId.isBlank()) {
        return badRequest("twinId is required");
      }

      Optional<EntityTwin> twin = MessageDaemon.getInstance().getSubSystemManager().getTwinManager().getTwin(twinId);
      if (twin.isEmpty()) {
        return notFound("Unknown twinId: " + twinId);
      }
      return ok(twin.get());
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin error");
    }
  }

  private Response mapAuthOrRethrow(WebApplicationException exception) {
    Response response = exception.getResponse();
    int status = response == null ? 500 : response.getStatus();
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