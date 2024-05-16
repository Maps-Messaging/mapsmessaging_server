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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.data.interfaces.InterfaceStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfacesStatusApi extends BaseInterfaceApi {


  @GET
  @Path("/server/interface/{endpoint}/status")
  @Produces({MediaType.APPLICATION_JSON})
  public InterfaceStatus getInterfaceStatus(@PathParam("endpoint") String endpointName) {
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
  public List<InterfaceStatus> getAllInterfaceStatus() {
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    List<InterfaceStatus> results = new ArrayList<>();
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    for (EndPointManager endPointManager : endPointManagers) {
      results.add(new InterfaceStatus(endPointManager.getEndPointServer()));
    }
    return results;
  }
}
