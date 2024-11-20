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
import io.mapsmessaging.dto.helpers.IntegrationInfoHelper;
import io.mapsmessaging.dto.rest.integration.IntegrationInfoDTO;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.rest.api.impl.interfaces.BaseInterfaceApi;
import io.mapsmessaging.rest.responses.EndPointDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Tag(name = "Server integration Management")
@Path(URI_PATH)
public class IntegrationInstanceApi extends BaseInterfaceApi {

  @GET
  @Path("/server/integration/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the endpoint current status and configuration")
  public IntegrationInfoDTO getIntegration(@PathParam("name") String name) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    EndPointConnection endPointConnection = locateInstance(name);
    if(endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    return IntegrationInfoHelper.fromEndPointConnection(endPointConnection);
  }


  @GET
  @Path("/server/integration/{name}/connection")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the endpoint current status and configuration")
  public EndPointDetails getIntegrationConnection(@PathParam("name") String name) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    EndPointConnection endPointConnection = locateInstance(name);
    if(endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    if(endPointConnection.getEndPoint() != null) {
      return new EndPointDetails(name, endPointConnection.getEndPoint());
    }
    return new EndPointDetails();
  }

  @PUT
  @Path("/server/integration/{name}/stop")
  //@ApiOperation(value = "Stops the specified endpoint and closes existing connections")
  public Response stopIntegration(@PathParam("name") String name) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    EndPointConnection endPointConnection = locateInstance(name);
    if(endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    endPointConnection.stop();
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/integration/{name}/start")
  //@ApiOperation(value = "Starts the specified endpoint")
  public Response startIntegration(@PathParam("name") String name) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    EndPointConnection endPointConnection = locateInstance(name);
    if(endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    endPointConnection.start();
    return Response.noContent().build();
  }


  @PUT
  @Path("/server/integration/{name}/resume")
  //@ApiOperation(value = "Resumes the specified endpoint if the endpoint had been paused")
  public Response resumeIntegration(@PathParam("name") String name) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    EndPointConnection endPointConnection = locateInstance(name);
    if(endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    endPointConnection.resume();
    return Response.noContent().build();
  }

  @PUT
  @Path("/server/integration/{name}/pause")
  //@ApiOperation(value = "Pauses the specified endpoint, existing connections are maintained but no new connections can be made")
  public Response pauseIntegration(@PathParam("name") String name) {
    checkAuthentication();
    if (!hasAccess("interfaces")) {
      response.setStatus(403);
      return null;
    }
    EndPointConnection endPointConnection = locateInstance(name);
    if(endPointConnection == null) {
      response.setStatus(404);
      return null;
    }
    endPointConnection.pause();
    return Response.noContent().build();
  }


  private EndPointConnection locateInstance(String name){
    List<EndPointConnection> list = MessageDaemon.getInstance().getNetworkConnectionManager().getEndPointConnectionList();
    for (EndPointConnection endPointConnection : list) {
      if(endPointConnection.getConfigName().equalsIgnoreCase(name)){
        return endPointConnection;
      }
    }
    return null;
  }
}
