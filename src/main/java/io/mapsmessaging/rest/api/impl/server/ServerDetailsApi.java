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

package io.mapsmessaging.rest.api.impl.server;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.ServerRunner;
import io.mapsmessaging.rest.api.impl.interfaces.BaseInterfaceApi;
import io.mapsmessaging.rest.data.StatusMessage;
import io.mapsmessaging.rest.responses.ServerStatisticsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Management")
@Path(URI_PATH)
public class ServerDetailsApi extends BaseInterfaceApi {

  @GET
  @Path("/server/details/info")
  @Produces({MediaType.APPLICATION_JSON})
  // @ApiOperation(value = "Returns the servers unique name")
  public StatusMessage getBuildInfo() {
    if (!hasAccess("servers")) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon messageDaemon = MessageDaemon.getInstance();
    return new StatusMessage(messageDaemon);
  }

  @GET
  @Path("/server/details/stats")
  @Produces({MediaType.APPLICATION_JSON})
//  @ApiOperation(value = "Retrieve the server statistics")
  public ServerStatisticsResponse getStats() {
    if (!hasAccess("servers")) {
      response.setStatus(403);
      return null;
    }
    return new ServerStatisticsResponse(request);
  }

  @GET
  @Path("/server/restart")
  @Produces({MediaType.APPLICATION_JSON})
//  @ApiOperation(value = "Retrieve the server statistics")
  public String restartServer() {
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
