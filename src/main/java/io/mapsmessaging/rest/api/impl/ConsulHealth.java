package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Server Health")
@Path("/health")
public class ConsulHealth extends BaseRestApi {

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(
      summary = "Check server health",
      description = "Checks the health of all subsystems and returns their overall status. Possible values are 'Ok', 'Warning', or 'Error'.",
      security = {},
      responses = {
          @ApiResponse(responseCode = "200", description = "Server is healthy"),
          @ApiResponse(responseCode = "503", description = "Server is unhealthy")
      })
  public Response getHealth() {
    String state = "Ok";

    for (SubSystemStatusDTO status : MessageDaemon.getInstance().getSubSystemManager().getSubSystemStatus()) {
      switch (status.getStatus()) {
        case ERROR:
          return Response.status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Error")
              .build();

        case WARN:
          state = "Warning";
          break;

        default:
          break;
      }
    }

    return Response.ok(state).build();
  }
}