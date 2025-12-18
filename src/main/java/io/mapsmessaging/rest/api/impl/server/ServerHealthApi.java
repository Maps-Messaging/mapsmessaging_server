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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.rest.EndpointInfo;
import io.mapsmessaging.rest.EndpointRegistry;
import io.mapsmessaging.rest.api.impl.interfaces.RequestedAction;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.ServerHealthStateResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.SubSystemStatusList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Management")
@Path(URI_PATH+"/server")
public class ServerHealthApi extends ServerBaseRestApi {

  @GET
  @Path("/status")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server subsystem status",
      description = "Retrieves the current status of all server subsystems, including their operational state (e.g., OK, Warning, or Error). Uses caching for improved performance.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubSystemStatusList.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public SubSystemStatusList getServerStatus() {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "serverStats");
    SubSystemStatusList cachedResponse = getFromCache(key, SubSystemStatusList.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch and cache response
    SubSystemStatusList response = new SubSystemStatusList( MessageDaemon.getInstance().getSubSystemStatus());
    putToCache(key, response);
    return response;
  }

  @GET
  @Path("/health")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get server subsystem status summary",
      description = "Returns a simple summary of the server status.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = ServerHealthStateResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public ServerHealthStateResponse getServerHealthSummary() {
    String state = "";
    int issueCount = 0;
    for (SubSystemStatusDTO status : MessageDaemon.getInstance().getSubSystemManager().getSubSystemStatus()) {
      switch (status.getStatus()) {
        case ERROR:
          state = "Error";
          issueCount++;
          break;

        case WARN:
          if (state.isEmpty()) {
            state = "Warning";
            issueCount++;
          }
          break;

        default:
          break;
      }
    }
    if (state.isEmpty()) {
      state = "Ok";
    }
    return new ServerHealthStateResponse(state, issueCount);
  }

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Restart or shutdown the server",
      description = "Restarts or shuts down the server gracefully, preserving any necessary state before the restart operation begins.",
      requestBody = @RequestBody(
          description = "Requested action to apply to all inter-server connections",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = RequestedAction.class)
          )
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse serverAction(@RequestBody  ServerAction requested, @Context HttpServletResponse httpServletResponse) {
    hasAccess(RESOURCE);
    if(requested != null && requested.getState() != null) {
      if (requested.getState().equalsIgnoreCase("restart")) {
        shutdown(8);
        return new StatusResponse("Restarting");
      } else if (requested.getState().equalsIgnoreCase("shutdown")) {
        shutdown(0);
        return new StatusResponse("Shutting down");
      }
    }
    httpServletResponse.setStatus(400);
    return new StatusResponse("Unknown action");
  }

  private void shutdown(int exitCode) {
    Runnable r = () -> {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      MessageDaemon.getInstance().stop(exitCode);
    };
    Thread t = new Thread(r);
    t.setDaemon(true);
    t.start();
  }


  @GET
  @Path("/endpoints")
  @Produces(MediaType.APPLICATION_JSON)
  public List<EndpointInfo> getEndpoints() {
    return EndpointRegistry.getInstance().getEndpoints();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServerAction{
    private String state;
  }
}
