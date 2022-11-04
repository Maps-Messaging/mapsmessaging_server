package io.mapsmessaging.rest.api.impl;

import static io.mapsmessaging.BuildInfo.buildVersion;
import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.rest.api.BaseRestApi;
import io.mapsmessaging.rest.responses.ServerStatisticsResponse;
import io.mapsmessaging.rest.responses.StringResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

}
