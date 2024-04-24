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

package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.responses.StringResponse;
import io.mapsmessaging.rest.responses.UpdateCheckResponse;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import javax.servlet.http.HttpServletResponse;

import static io.mapsmessaging.BuildInfo.buildVersion;
import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@OpenAPIDefinition(
    info = @Info(
        description = "Maps Messaging Server Rest API, provides simple Rest API to manage and interact with the server",
        version = buildVersion,
        title = "Maps Messaging Rest Server",
        contact = @Contact(
            name = "Matthew Buckton",
            email = "info@mapsmessaging.io",
            url = "http://mapsmessaging.io"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "http://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    //schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
    tags = {
        @Tag(name = "Authentication and Authorisation Management", description = "Managers authentication and authorisation of users on the server"),
        @Tag(name = "Destination Management", description = "Used to manage the destinations (topic/queues) and the subscriptions"),
        @Tag(name = "Messaging Interface", description = "Used to send and receive messages from the server"),
        @Tag(name = "Server Health", description = "Simple server health endpoint"),
        @Tag(name = "Server Interface Management", description = "Used to manage the servers network interfaces"),
        @Tag(name = "Schema Management", description = "Used to manage the schemas configured on the server"),
        @Tag(name = "Server Management", description = "Server status and management"),
        @Tag(name = "Server Integration Management", description = "Manages interconnections with other brokers"),
        @Tag(name = "Connection Management", description = "Manages client connections")
    },
    externalDocs = @ExternalDocumentation(description = "Maps Messaging", url = "https://www.mapsmessaging.io/")
)
@Tag(name = "Server Health")
@Path(URI_PATH)
public class MapsRestServerApi extends BaseRestApi {

  @GET
  @Path("/ping")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Simple request to test if the server is running")
  public StringResponse getPing() {
    return new StringResponse(request, "ok");
  }

  @GET
  @Path("/name")
  @Produces({MediaType.APPLICATION_JSON})
  // @ApiOperation(value = "Returns the servers unique name")
  public StringResponse getName() {
    return new StringResponse(request, MessageDaemon.getInstance().getId());
  }

  @GET
  @Path("/updates")
  @Produces({MediaType.APPLICATION_JSON})
  // @ApiOperation(value = "Check for changes to the configuration update counts")
  public UpdateCheckResponse checkForUpdates() {
    long schema = SchemaManager.getInstance().getUpdateCount();
    return new UpdateCheckResponse(schema, 0, 0);
  }

  @GET
  @Path("/login")
  @Produces({MediaType.APPLICATION_JSON})
  // @ApiOperation(value = "Check for changes to the configuration update counts")
  public String login() {
    HttpSession session = request.getSession(false);
    if(session != null){
      if (!hasAccess("root")) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "{\"Status\": \"No Access\"}";
      }
      return "{\"Status\": \"OK\"}";
    }
    return "{\"Status\": \"No Authentication Required\"}";
  }

  @GET
  @Path("/logout")
  @Produces({MediaType.APPLICATION_JSON})
  // @ApiOperation(value = "Check for changes to the configuration update counts")
  public String logout() {
    HttpSession session = request.getSession(false);
    if(session != null){
      session.invalidate();
    }
    return "{\"Status\": \"OK\"}";
  }

}
