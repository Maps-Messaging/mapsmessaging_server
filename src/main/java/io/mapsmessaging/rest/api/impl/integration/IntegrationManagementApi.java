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
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.integration.IntegrationInfo;
import io.mapsmessaging.rest.responses.IntegrationDetailResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Integration Management")
@Path(URI_PATH)
public class IntegrationManagementApi  extends BaseRestApi {

  @GET
  @Path("/server/integration")
  @Produces({MediaType.APPLICATION_JSON})
  public IntegrationDetailResponse getAllIntegrations() {
    if (!hasAccess("integrations")) {
      response.setStatus(403);
      return null;
    }

    List<EndPointConnection> endPointManagers = MessageDaemon.getInstance().getNetworkConnectionManager().getEndPointConnectionList();
    List<IntegrationInfo> protocols = new ArrayList<>();
    ConfigurationProperties global = null;
    for (EndPointConnection endPointConnection : endPointManagers) {
      protocols.add(new IntegrationInfo(endPointConnection));
    }
    return new IntegrationDetailResponse(request, protocols, global);
  }
}
