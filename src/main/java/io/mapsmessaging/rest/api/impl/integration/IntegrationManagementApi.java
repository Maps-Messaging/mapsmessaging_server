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

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.integration.IntegrationInfo;
import io.mapsmessaging.rest.responses.IntegrationDetailResponse;
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

@Tag(name = "Server Integration Management")
@Path(URI_PATH)
public class IntegrationManagementApi  extends BaseRestApi {

  @GET
  @Path("/server/integration")
  @Produces({MediaType.APPLICATION_JSON})
  public IntegrationDetailResponse getAllIntegrations(@QueryParam("filter") String filter) throws ParseException {
    checkAuthentication();
    if (!hasAccess("integrations")) {
      response.setStatus(403);
      return null;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty())  ? SelectorParser.compile(filter) : null;
    List<EndPointConnection> endPointManagers = MessageDaemon.getInstance().getNetworkConnectionManager().getEndPointConnectionList();
    ConfigurationProperties global = null;
    List<IntegrationInfo> protocols = endPointManagers.stream()
        .map(IntegrationInfo::new)
        .filter(info -> parser == null || parser.evaluate(info))
        .collect(Collectors.toList());
    return new IntegrationDetailResponse(request, protocols, global);
  }
}
