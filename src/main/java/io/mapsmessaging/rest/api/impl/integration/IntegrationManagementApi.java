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

package io.mapsmessaging.rest.api.impl.integration;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.helpers.IntegrationInfoHelper;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.IntegrationDetailResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Server Integration Management")
@Path(URI_PATH)
public class IntegrationManagementApi  extends BaseRestApi {
  private static final String INTERFACES = "Interfaces";

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
    List<EndPointConnection> endPointManagers = MessageDaemon.getInstance().getSubSystemManager().getNetworkConnectionManager().getEndPointConnectionList();
    ConfigurationProperties global = null;
    List<IntegrationInfoDTO> protocols =
        endPointManagers.stream()
            .map(IntegrationInfoHelper::fromEndPointConnection)
            .filter(info -> parser == null || parser.evaluate(info))
            .collect(Collectors.toList());
    return new IntegrationDetailResponse(protocols, global);
  }

  @PUT
  @Path("/server/integration/stopAll")
  //@ApiOperation(value = "Stops all all configured interfaces")
  public Response stopAllInterfaces() {
    checkAuthentication();
    if (!hasAccess(INTERFACES)) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getSubSystemManager().getNetworkConnectionManager().stop();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/integration/startAll")
  //@ApiOperation(value = "Starts all all configured interfaces")
  public Response startAllInterfaces() {
    checkAuthentication();
    if (!hasAccess(INTERFACES)) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getSubSystemManager().getNetworkConnectionManager().start();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/integration/pauseAll")
  //@ApiOperation(value = "Pauses all all configured interfaces")
  public Response pauseAllInterfaces() {
    checkAuthentication();
    if (!hasAccess(INTERFACES)) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getSubSystemManager().getNetworkConnectionManager().pause();
    return Response.ok().build();
  }


  @PUT
  @Path("/server/integration/resumeAll")
  //@ApiOperation(value = "Resumes all all configured interfaces")
  public Response resumeAllInterfaces() {
    checkAuthentication();
    if (!hasAccess(INTERFACES)) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getSubSystemManager().getNetworkConnectionManager().resume();
    return Response.ok().build();
  }
}
