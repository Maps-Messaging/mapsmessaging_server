/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Discovery Management")
@Path(URI_PATH)
public class DiscoveryManagementApi extends BaseRestApi {

  @GET
  @Path("/server/discovery/start")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public boolean startDiscovery() {
    checkAuthentication();
    if (!hasAccess("discovery")) {
      response.setStatus(403);
      return false;
    }
    MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().start();
    return true;
  }

  @GET
  @Path("/server/discovery/stop")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public boolean stopDiscovery() {
    checkAuthentication();
    if (!hasAccess("discovery")) {
      response.setStatus(403);
      return false;
    }
    MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().stop();
    return true;
  }

  @GET
  @Path("/server/discovery")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public List<DiscoveredServersDTO> getAllDiscoveredServers(@QueryParam("filter") String filter) throws ParseException {
    checkAuthentication();
    if (!hasAccess("discovery")) {
      response.setStatus(403);
      return new ArrayList<>();
    }
    // Create cache key
    CacheKey key = new CacheKey(uriInfo.getPath(), ((filter != null && !filter.isEmpty()) ? ""+filter.hashCode() : "") );

    // Try to retrieve from cache
    @SuppressWarnings("unchecked")
    List<DiscoveredServersDTO> cachedResponse = getFromCache(key, List.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty())  ? SelectorParser.compile(filter) : SelectorParser.compile("true");
    List<DiscoveredServersDTO> result = MessageDaemon.getInstance().getSubSystemManager().getServerConnectionManager().getServers().stream().filter(parser::evaluate).collect(Collectors.toList());
    putToCache(key, result);
    return result;
  }
}
