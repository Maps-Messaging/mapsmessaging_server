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

package io.mapsmessaging.rest.api.impl.integration;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.interfaces.BaseInterfaceApi;
import io.mapsmessaging.rest.data.integration.IntegrationStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Integration Management")
@Path(URI_PATH)
public class IntergrationStatusApi extends BaseInterfaceApi {


  @GET
  @Path("/server/integration/{endpoint}/status")
  @Produces({MediaType.APPLICATION_JSON})
  public IntegrationStatus getIntegrationStatus(@PathParam("endpoint") String endpointName) {
    List<EndPointConnection> endPointManagers = MessageDaemon.getInstance().getNetworkConnectionManager().getEndPointConnectionList();
    for (EndPointConnection endPointConnection : endPointManagers) {
      if (endpointName.equals(endPointConnection.getConfigName())) {
        return new IntegrationStatus(endPointConnection);
      }
    }
    return null;
  }

  @GET
  @Path("/server/integration/status")
  @Produces({MediaType.APPLICATION_JSON})
  public List<IntegrationStatus> getAllIntegrationStatus() {
    List<EndPointConnection> endPointManagers = MessageDaemon.getInstance().getNetworkConnectionManager().getEndPointConnectionList();
    List<IntegrationStatus> results = new ArrayList<>();
    for (EndPointConnection endPointConnection : endPointManagers) {
      results.add(new IntegrationStatus(endPointConnection));
    }
    return results;
  }
}
