/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.rest.api.impl.destination.BaseDestinationApi;
import io.mapsmessaging.rest.responses.EndPointDetailResponse;
import io.mapsmessaging.rest.responses.EndPointDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Connection Management")
@Path(URI_PATH)
public class ConnectionManagementApi extends BaseDestinationApi {
  @GET
  @Path("/server/connections")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public EndPointDetailResponse getAllConnections() {
    if (!hasAccess("connections")) {
      response.setStatus(403);
      return null;
    }

    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    List<EndPointDetails> endPointDetails = new ArrayList<>();
    for (EndPointManager endPointManager : endPointManagers) {
      for(EndPoint  endPoint:endPointManager.getEndPointServer().getActiveEndPoints()) {
        endPointDetails.add(new EndPointDetails(endPointManager.getName(), endPoint));
      }
    }

    return new EndPointDetailResponse(request, endPointDetails);
  }
}
