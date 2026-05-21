package io.mapsmessaging.rest.api.impl.license;

import io.mapsmessaging.license.FeatureDetails;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Path(URI_PATH + "/license")
public class LicenseApi {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get license",
      description = "Returns the current license details.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "License retrieved successfully",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = FeatureDetails.class)
              )
          ),
          @ApiResponse(
              responseCode = "404",
              description = "License not found",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response getLicense() {
    FeatureDetails license = ConfigurationManager.getInstance().getFeatureManager().loadLicense();
    if (license == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("License not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    return Response.ok(license, MediaType.APPLICATION_JSON).build();
  }
}