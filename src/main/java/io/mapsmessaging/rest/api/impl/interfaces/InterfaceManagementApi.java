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
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.interfaces.InterfaceInfo;
import io.mapsmessaging.rest.responses.InterfaceDetailResponse;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Server Interface Management")
@Path(URI_PATH)
public class InterfaceManagementApi extends BaseRestApi {

  @GET
  @Path("/server/interfaces")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Retrieve a list of all configured interfaces")
  public InterfaceDetailResponse getAllInterfaces(@QueryParam("filter") String filter) throws ParseException {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty())  ? SelectorParser.compile(filter) : null;
    List<EndPointManager> endPointManagers = MessageDaemon.getInstance().getNetworkManager().getAll();
    ConfigurationProperties global = endPointManagers.stream()
          .findFirst()
          .map(endPointManager -> endPointManager.getEndPointServer().getConfig().getProperties().getGlobal())
          .orElse(null);

    List<InterfaceInfo> protocols = endPointManagers.stream()
        .map(InterfaceInfo::new)
        .filter(protocol -> parser == null || parser.evaluate(protocol))
        .collect(Collectors.toList());
    return new InterfaceDetailResponse(request, protocols, global);
  }


  @PUT
  @Path("/server/interfaces/stopAll")
  //@ApiOperation(value = "Stops all all configured interfaces")
  public Response stopAllInterfaces() {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getNetworkManager().stopAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/startAll")
  //@ApiOperation(value = "Starts all all configured interfaces")
  public Response startAllInterfaces() {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getNetworkManager().startAll();
    return Response.ok().build();
  }

  @PUT
  @Path("/server/interfaces/pauseAll")
  //@ApiOperation(value = "Pauses all all configured interfaces")
  public Response pauseAllInterfaces() {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getNetworkManager().pauseAll();
    return Response.ok().build();
  }


  @PUT
  @Path("/server/interfaces/resumeAll")
  //@ApiOperation(value = "Resumes all all configured interfaces")
  public Response resumeAllInterfaces() {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getNetworkManager().resumeAll();
    return Response.ok().build();
  }
}
