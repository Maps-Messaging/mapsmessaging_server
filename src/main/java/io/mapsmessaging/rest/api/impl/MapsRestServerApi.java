/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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
import io.mapsmessaging.rest.api.BaseRestApi;
import io.mapsmessaging.rest.responses.ServerStatisticsResponse;
import io.mapsmessaging.rest.responses.StringResponse;
import io.mapsmessaging.rest.responses.UpdateCheckResponse;
import io.swagger.annotations.*;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static io.mapsmessaging.BuildInfo.buildVersion;
import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@SwaggerDefinition(
    info = @Info(
        description = "Maps Messaging Server Rest API, provides simple Rest API to manage the server",
        version = buildVersion,
        title = "Maps Messaging Rest Server",
        contact = @Contact(
            name = "Matthew Buckton",
            email = "matthew.bucktone@mapsmessaging.io",
            url = "http://mapsmessaging.io"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "http://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    consumes = {"application/json", "application/xml"},
    produces = {"application/json", "application/xml"},
    schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
    tags = {
        @Tag(name = "Server Interface Management", description = "Used to manage the servers network interfaces"),
        @Tag(name = "Destination Management", description = "Used to manage the destinations (topic/queues) and the subscriptions"),
        @Tag(name = "Destination Statistics Management", description = "Used to retrieve the destinations (topic/queues) statistics"),
        @Tag(name = "Interface Management", description = "Used to manage an individual network interface"),
        @Tag(name = "Schema Management", description = "Used to manage the schemas configured on the server"),
        @Tag(name = "Messaging Server API", description = "Global APIs to manage and query the server"),

    },
    externalDocs = @ExternalDocs(value = "Maps Messaging", url = "https://www.mapsmessaging.io/")
)
@Api(value = URI_PATH, tags="Messaging Server API")
@Path(URI_PATH)
public class MapsRestServerApi extends BaseRestApi {

  @GET
  @Path("/ping")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Simple request to test if the server is running")
  public StringResponse getPing() {
    return new StringResponse(request, "ok");
  }

  @GET
  @Path("/name")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Returns the servers unique name")
  public StringResponse getName() {
    return new StringResponse(request, MessageDaemon.getInstance().getId());
  }

  @GET
  @Path("/stats")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Retrieve the server statistics")
  public ServerStatisticsResponse getStats() {
    return new ServerStatisticsResponse(request);
  }

  @GET
  @Path("/updates")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Check for changes to the configuration update counts")
  public UpdateCheckResponse checkForUpdates() {
    long schema = SchemaManager.getInstance().getUpdateCount();
    return new UpdateCheckResponse(schema, 0, 0);
  }


}
