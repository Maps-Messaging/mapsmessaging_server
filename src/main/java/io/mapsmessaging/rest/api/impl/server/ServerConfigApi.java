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

package io.mapsmessaging.rest.api.impl.server;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import io.mapsmessaging.rest.api.impl.interfaces.BaseInterfaceApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Server Config Management")
@Path(URI_PATH)
public class ServerConfigApi  extends BaseInterfaceApi {

  @GET
  @Path("/server/config")
  @Produces({MediaType.APPLICATION_JSON})
  // @ApiOperation(value = "Returns the servers unique name")
  public MessageDaemonConfigDTO getServerConfig() {
    checkAuthentication();
    if (!hasAccess("servers")) {
      response.setStatus(403);
      return null;
    }
    return MessageDaemon.getInstance().getMessageDaemonConfig();
  }

  @PUT
  @Path("/server/config")
  @Produces({MediaType.APPLICATION_JSON})
  public Response updateServerConfig(MessageDaemonConfigDTO dto) {
    checkAuthentication();
    if (!hasAccess("servers")) {
      response.setStatus(403);
      return null;
    }
    MessageDaemon.getInstance().getMessageDaemonConfig().update(dto);
    return Response.ok().build();
  }

}
