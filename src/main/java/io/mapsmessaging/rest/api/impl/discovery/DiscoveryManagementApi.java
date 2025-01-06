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

package io.mapsmessaging.rest.api.impl.discovery;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.discovery.DiscoveredServersDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Discovery Management")
@Path(URI_PATH)
public class DiscoveryManagementApi extends DiscoveryBaseRestApi {

  @GET
  @Path("/server/discovery/start")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Start Discovery agent",
      description = "Starts the mDNS discovery sub-system. Requires authentication if enabled in the configuration."
  )
  public boolean startDiscovery() {
    hasAccess(RESOURCE);
    MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().start();
    return true;
  }

  @GET
  @Path("/server/discovery/stop")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Stop Discovery daemon",
      description = "Stops the mDNS discovery sub-system. Requires authentication if enabled in the configuration."
  )
  public boolean stopDiscovery() {
    hasAccess(RESOURCE);
    MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().stop();
    return true;
  }

  @GET
  @Path("/server/discovery")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get discovered servers",
      description = "Retrieve a list of all currently discovered servers, can be filtered with the optional filter. Requires authentication if enabled in the configuration."
  )  public List<DiscoveredServersDTO> getAllDiscoveredServers(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type= "String", example = "schemaSupport = TRUE OR systemTopicPrefix IS NOT NULL")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), ((filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : ""));

    @SuppressWarnings("unchecked")
    List<DiscoveredServersDTO> cachedResponse = getFromCache(key, List.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : SelectorParser.compile("true");
    List<DiscoveredServersDTO> result =
        MessageDaemon.getInstance()
            .getSubSystemManager()
            .getServerConnectionManager()
            .getServers()
            .stream()
            .filter(parser::evaluate)
            .collect(Collectors.toList());
    putToCache(key, result);
    return result;
  }
}
