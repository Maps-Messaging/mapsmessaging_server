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

package io.mapsmessaging.rest.api.impl.interfaces;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.data.interfaces.InterfaceStatus;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfacesStatusApi extends BaseInterfaceApi {


  @GET
  @Path("/server/interface/{endpoint}/status")
  @Produces({MediaType.APPLICATION_JSON})
  public InterfaceStatus getInterfaceStatus(@PathParam("endpoint") String endpointName) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      if (isMatch(endpointName, endPointManager)) {
        return new InterfaceStatus(endPointManager.getEndPointServer());
      }
    }
    return null;
  }

  @GET
  @Path("/server/interface/status")
  @Produces({MediaType.APPLICATION_JSON})
  public List<InterfaceStatus> getAllInterfaceStatus(@QueryParam("filter") String filter) throws ParseException {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty())  ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();

    return endPointManagers.stream()
        .map(endPointManager -> new InterfaceStatus(endPointManager.getEndPointServer()))
        .filter(status -> parser == null || parser.evaluate(status))
        .collect(Collectors.toList());
  }
}
