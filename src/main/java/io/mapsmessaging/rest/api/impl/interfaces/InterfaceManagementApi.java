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

package io.mapsmessaging.rest.api.impl.interfaces;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.helpers.InterfaceInfoHelper;
import io.mapsmessaging.dto.rest.interfaces.InterfaceInfoDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.InterfaceDetailResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfaceManagementApi extends BaseInterfaceApi {

  @GET
  @Path("/server/interfaces")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all end point details",
      description = "get all end point configuration details, filtered with the optional filter."
  )
  public InterfaceDetailResponse getAllInterfaces(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "state = 'started'")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "");
    InterfaceDetailResponse cachedResponse = getFromCache(key, InterfaceDetailResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers =
        MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().getAll();

    List<InterfaceInfoDTO> protocols =
        endPointManagers.stream()
            .map(InterfaceInfoHelper::fromEndPointManager)
            .filter(protocol -> parser == null || parser.evaluate(protocol))
            .collect(Collectors.toList());

    InterfaceDetailResponse response = new InterfaceDetailResponse(protocols);

    // Cache the response
    putToCache(key, response);
    return response;
  }

  @PUT
  @Path("/server/interfaces/stopAll")
  @Operation(
      summary = "Stop all end points",
      description = "Stops all running endpoints."
  )
  public Response stopAllInterfaces() {
    hasAccess(RESOURCE);
    MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().stopAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/startAll")
  @Operation(
      summary = "Start all end points",
      description = "Starts all stopped endpoints."
  )
  public Response startAllInterfaces() {
    hasAccess(RESOURCE);
    MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().startAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/pauseAll")
  @Operation(
      summary = "Pause all end points",
      description = "Pauses all running endpoints and stops new incoming connections."
  )
  public Response pauseAllInterfaces() {
    hasAccess(RESOURCE);
    MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().pauseAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/resumeAll")
  @Operation(
      summary = "Resume all end points",
      description = "Resumes all paused endpoints and allows new incoming connections."
  )
  public Response resumeAllInterfaces() {
    hasAccess(RESOURCE);
    MessageDaemon.getInstance().getSubSystemManager().getNetworkManager().resumeAll();
    return Response.ok().build();
  }
}
