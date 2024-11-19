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

package io.mapsmessaging.rest.api.impl.connections;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.api.impl.destination.BaseDestinationApi;
import io.mapsmessaging.rest.responses.EndPointDetailResponse;
import io.mapsmessaging.rest.responses.EndPointDetails;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Connection Management")
@Path(URI_PATH)
public class ConnectionManagementApi extends BaseDestinationApi {
  @GET
  @Path("/server/connections")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public EndPointDetailResponse getAllConnections(@QueryParam("filter") String filter) throws ParseException {
    if (!hasAccess("connections")) {
      response.setStatus(403);
      return null;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty())  ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    List<EndPointDetails> endPointDetails = endPointManagers.stream()
        .flatMap(endPointManager -> endPointManager.getEndPointServer().getActiveEndPoints().stream()
            .map(endPoint -> new EndPointDetails(endPointManager.getName(), endPoint)))
        .filter(endPointDetail -> parser == null || parser.evaluate(endPointDetail))
        .collect(Collectors.toList());

    return new EndPointDetailResponse(request, endPointDetails);
  }
}
