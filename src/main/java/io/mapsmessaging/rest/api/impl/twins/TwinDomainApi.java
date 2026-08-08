/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.geospatial.GeoSpatialAreaConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;

@Tag(name = "Server Twin Administration", description = "Additive administrative REST facade over the existing TwinManager configuration model.")
@Path(URI_PATH + "/server/twin")
@Produces(MediaType.APPLICATION_JSON)
public class TwinDomainApi extends BaseRestApi {

  private static final String RESOURCE = "server/twin/config";

  @GET
  @Path("/drones")
  @Operation(summary = "List administrative drone configurations", responses = @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DroneAdminDTO.class)))))
  public Response listDrones() {
    return read(() -> service().listDrones());
  }

  @GET
  @Path("/drones/{name}")
  @Operation(summary = "Get one administrative drone configuration")
  public Response getDrone(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      return service().getDrone(name).map(this::ok).orElseGet(() -> notFound("Unknown drone configuration: " + name));
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin administration error");
    }
  }

  @POST
  @Path("/drones")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Create a drone and its existing-config transport mapping")
  public Response createDrone(DroneAdminDTO request) {
    try {
      hasAccess(RESOURCE);
      DroneAdminDTO created = service().createDrone(request);
      removeUriFromCache(uriInfo.getPath());
      return Response.status(Response.Status.CREATED).entity(created).build();
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin administration error");
    }
  }

  @PUT
  @Path("/drones/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update a drone and its existing-config transport mapping")
  public Response updateDrone(@PathParam("name") String name, DroneAdminDTO request) {
    try {
      hasAccess(RESOURCE);
      DroneAdminDTO updated = service().updateDrone(name, request);
      removeUriFromCache(uriInfo.getPath());
      return ok(updated);
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin administration error");
    }
  }

  @DELETE
  @Path("/drones/{name}")
  @Operation(summary = "Delete a drone and remove its MAVLink or CAN/N2K mapping")
  public Response deleteDrone(@PathParam("name") String name) {
    return mutateNoContent(() -> service().deleteDrone(name));
  }

  @GET
  @Path("/authorities")
  @Operation(summary = "List authority UUIDs and their derived drone capability bindings")
  public Response listAuthorities() {
    return read(() -> service().listAuthorities());
  }

  @GET
  @Path("/authorities/{uuid}")
  @Operation(summary = "Get one authority UUID and its derived drone capability bindings")
  public Response getAuthority(@PathParam("uuid") UUID uuid) {
    try {
      hasAccess(RESOURCE);
      return service().getAuthority(uuid).map(this::ok).orElseGet(() -> notFound("Unknown authority: " + uuid));
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin administration error");
    }
  }

  @PUT
  @Path("/authorities/{uuid}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Atomically add or remove full-drone bindings for an authority UUID while preserving untouched capability-level exceptions")
  public Response updateAuthorityBindings(@PathParam("uuid") UUID uuid, AuthorityBindingDTO request) {
    try {
      hasAccess(RESOURCE);
      AuthoritySummaryDTO updated = service().updateAuthorityBindings(uuid, request);
      removeUriFromCache(uriInfo.getPath());
      return ok(updated);
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin administration error");
    }
  }

  @DELETE
  @Path("/authorities/{uuid}")
  @Operation(summary = "Remove an authority UUID from every configured drone task capability")
  public Response deleteAuthority(@PathParam("uuid") UUID uuid) {
    return mutateNoContent(() -> service().deleteAuthority(uuid));
  }

  @GET
  @Path("/drones/{name}/authorities")
  @Operation(summary = "List authority UUIDs referenced by a drone's task capabilities")
  public Response listDroneAuthorities(@PathParam("name") String name) {
    return read(() -> service().listDroneAuthorities(name));
  }

  @PUT
  @Path("/drones/{name}/authorities/{uuid}")
  @Operation(summary = "Add an authority UUID to every task capability configured on a drone")
  public Response putDroneAuthority(@PathParam("name") String name, @PathParam("uuid") UUID uuid) {
    return mutateNoContent(() -> service().putDroneAuthority(name, uuid));
  }

  @DELETE
  @Path("/drones/{name}/authorities/{uuid}")
  @Operation(summary = "Remove an authority UUID from every task capability configured on a drone")
  public Response deleteDroneAuthority(@PathParam("name") String name, @PathParam("uuid") UUID uuid) {
    return mutateNoContent(() -> service().deleteDroneAuthority(name, uuid));
  }

  @GET
  @Path("/models")
  @Operation(summary = "List installed UxV models and supported operations")
  public Response listModels() {
    return read(() -> service().listModels());
  }

  @GET
  @Path("/models/{name}")
  @Operation(summary = "Get one installed UxV model and its supported operations")
  public Response getModel(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      return service().getModel(name).map(this::ok).orElseGet(() -> notFound("Unknown UxV model: " + name));
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin administration error");
    }
  }

  @GET
  @Path("/geospatial/areas")
  @Operation(summary = "List named geospatial areas available for drone assignment", responses = @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GeoSpatialAreaConfigDTO.class)))))
  public Response listGeospatialAreas() {
    return read(() -> service().listGeospatialAreas());
  }

  @GET
  @Path("/mavlink/sources")
  @Operation(summary = "List existing MAVLink source definitions available for drone mappings", responses = @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MavlinkTwinConfigDTO.class)))))
  public Response listMavlinkSources() {
    return read(() -> service().listMavlinkSources());
  }

  private TwinDomainService service() {
    TwinManagerConfig config = TwinManagerConfig.getInstance();
    if (config == null) {
      throw new IllegalStateException("TwinManager configuration is not available");
    }
    return new TwinDomainService(config);
  }

  private Response read(ReadAction action) {
    try {
      hasAccess(RESOURCE);
      return ok(action.run());
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin administration error");
    }
  }

  private Response mutateNoContent(SaveAction action) {
    try {
      hasAccess(RESOURCE);
      action.run();
      removeUriFromCache(uriInfo.getPath());
      return noContent();
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin administration error");
    }
  }

  private Response status(TwinConfigurationStore.TwinConfigurationException exception) {
    return Response.status(exception.getStatusCode()).entity(new StatusResponse(exception.getMessage())).type(MediaType.APPLICATION_JSON).build();
  }

  private Response mapAuthOrRethrow(WebApplicationException exception) {
    Response response = exception.getResponse();
    int status = response == null ? 500 : response.getStatus();
    if (status == 401) {
      return Response.status(Response.Status.UNAUTHORIZED).entity(new StatusResponse("Unauthorized")).type(MediaType.APPLICATION_JSON).build();
    }
    if (status == 403) {
      return Response.status(Response.Status.FORBIDDEN).entity(new StatusResponse("Access denied")).type(MediaType.APPLICATION_JSON).build();
    }
    throw exception;
  }

  @FunctionalInterface
  private interface ReadAction {
    Object run();
  }

  @FunctionalInterface
  private interface SaveAction {
    void run() throws IOException;
  }
}
