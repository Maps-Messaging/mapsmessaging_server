/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging]
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

package io.mapsmessaging.rest.api.impl.server;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.ServerRunner;
import io.mapsmessaging.dto.helpers.ServerStatisticsHelper;
import io.mapsmessaging.dto.helpers.StatusMessageHelper;
import io.mapsmessaging.dto.rest.StatusMessageDTO;
import io.mapsmessaging.rest.api.impl.interfaces.BaseInterfaceApi;
import io.mapsmessaging.rest.responses.ServerStatisticsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "Server Management")
@Path(URI_PATH)
public class ServerDetailsApi extends BaseInterfaceApi {

  @GET
  @Path("/server/details/info")
  @Produces({MediaType.APPLICATION_JSON})
  // @ApiOperation(value = "Returns the servers unique name")
  public StatusMessageDTO getBuildInfo() {
    checkAuthentication();
    if (!hasAccess("servers")) {
      response.setStatus(403);
      return null;
    }
    return StatusMessageHelper.fromMessageDaemon(MessageDaemon.getInstance());
  }

  @GET
  @Path("/server/details/stats")
  @Produces({MediaType.APPLICATION_JSON})
//  @ApiOperation(value = "Retrieve the server statistics")
  public ServerStatisticsResponse getStats() {
    checkAuthentication();
    if (!hasAccess("servers")) {
      response.setStatus(403);
      return null;
    }
    return new ServerStatisticsResponse(request, ServerStatisticsHelper.create());
  }

  @GET
  @Path("/server/restart")
  @Produces({MediaType.APPLICATION_JSON})
//  @ApiOperation(value = "Retrieve the server statistics")
  public String restartServer() {
    checkAuthentication();
    if (!hasAccess("serverControl")) {
      response.setStatus(403);
      return "{\"status\":\"Not Authorised\"}";
    }
    shutdown(8);
    return "{\"status\":\"Restarting\"}";

  }

  @GET
  @Path("/server/shutdown")
  @Produces({MediaType.APPLICATION_JSON})
//  @ApiOperation(value = "Retrieve the server statistics")
  public String shutdownServer() {
    checkAuthentication();
    if (!hasAccess("serverControl")) {
      response.setStatus(403);
      return "{\"status\":\"Not Authorised\"}";
    }
    shutdown(0);
    return "{\"status\":\"Shutting down\"}";
  }

  private void shutdown(int exitCode){
    ServerRunner.getExitRunner().deletePidFile(exitCode);
  }
}
