/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.geospatial.GeoSpatialAreaConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
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
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Tag(name = "Server Twin Geospatial Administration", description = "Administration of the existing TwinManager geospatial area configuration and GeoJSON boundary files.")
@Path(URI_PATH + "/server/twin/admin/geospatial")
public class GeoSpatialAdministrationApi extends BaseRestApi {

  private static final String RESOURCE = "server/twin/config";
  private static final int PRECONDITION_REQUIRED = 428;

  @GET
  @Path("/areas")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "List configured geospatial areas")
  public Response listAreas() {
    try {
      hasAccess(RESOURCE);
      return ok(service().listAreas());
    } catch (WebApplicationException exception) {
      return mapAuthOrRethrow(exception);
    } catch (Exception exception) {
      return internalServerError("Server geospatial administration error");
    }
  }

  @GET
  @Path("/areas/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get one configured geospatial area")
  public Response getArea(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      return service().getArea(name).map(this::etagResponse).orElseGet(() -> notFound("Unknown geospatial area: " + name));
    } catch (TwinConfigurationStore.TwinConfigurationException exception) {
      return status(exception);
    } catch (WebApplicationException exception) {
      return mapAuthOrRethrow(exception);
    } catch (Exception exception) {
      return internalServerError("Server geospatial administration error");
    }
  }

  @POST
  @Path("/areas/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Create an empty named geospatial area")
  public Response createArea(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      TwinManagerConfig config = config();
      synchronized (config) {
        GeoSpatialAreaConfigDTO area = new GeoSpatialAdminService(config).createArea(name);
        removeUriFromCache(uriInfo.getPath());
        return Response.status(Response.Status.CREATED).entity(area).tag(TwinResourceEtag.of(area)).build();
      }
    } catch (TwinConfigurationStore.TwinConfigurationException exception) {
      return status(exception);
    } catch (WebApplicationException exception) {
      return mapAuthOrRethrow(exception);
    } catch (IOException exception) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception exception) {
      return internalServerError("Server geospatial administration error");
    }
  }

  @DELETE
  @Path("/areas/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Delete an unused geospatial area")
  public Response deleteArea(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      TwinManagerConfig config = config();
      synchronized (config) {
        GeoSpatialAdminService service = new GeoSpatialAdminService(config);
        GeoSpatialAreaConfigDTO current = service.getArea(name).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial area: " + name, 404));
        Response precondition = requireIfMatch(current);
        if (precondition != null) return precondition;
        service.deleteArea(name);
        removeUriFromCache(uriInfo.getPath());
        return noContent();
      }
    } catch (TwinConfigurationStore.TwinConfigurationException exception) {
      return status(exception);
    } catch (WebApplicationException exception) {
      return mapAuthOrRethrow(exception);
    } catch (IOException exception) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception exception) {
      return internalServerError("Server geospatial administration error");
    }
  }

  @PUT
  @Path("/areas/{areaName}/boundaries/{boundaryName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Upload or replace a GeoJSON boundary")
  public Response putBoundary(@PathParam("areaName") String areaName, @PathParam("boundaryName") String boundaryName, @FormDataParam("type") String type, @FormDataParam("file") InputStream fileStream) {
    try {
      hasAccess(RESOURCE);
      if (fileStream == null) return badRequest("GeoJSON file is required");
      GeoSpatialBoundaryType boundaryType;
      try {
        boundaryType = type == null || type.isBlank() ? GeoSpatialBoundaryType.INSIDE : GeoSpatialBoundaryType.valueOf(type.trim().toUpperCase());
      } catch (IllegalArgumentException exception) {
        return badRequest("Boundary type must be INSIDE or DO_NOT_ENTER");
      }
      byte[] content = fileStream.readAllBytes();
      TwinManagerConfig config = config();
      synchronized (config) {
        GeoSpatialAdminService service = new GeoSpatialAdminService(config);
        GeoSpatialAreaConfigDTO current = service.getArea(areaName).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial area: " + areaName, 404));
        Response precondition = requireIfMatch(current);
        if (precondition != null) return precondition;
        Object boundary = service.putBoundary(areaName, boundaryName, boundaryType, content);
        GeoSpatialAreaConfigDTO updatedArea = service.getArea(areaName).orElseThrow();
        removeUriFromCache(uriInfo.getPath());
        return Response.ok(boundary).tag(TwinResourceEtag.of(updatedArea)).build();
      }
    } catch (TwinConfigurationStore.TwinConfigurationException exception) {
      return status(exception);
    } catch (WebApplicationException exception) {
      return mapAuthOrRethrow(exception);
    } catch (IOException exception) {
      return internalServerError("Unable to store GeoJSON boundary");
    } catch (Exception exception) {
      return internalServerError("Server geospatial administration error");
    }
  }

  @GET
  @Path("/areas/{areaName}/boundaries/{boundaryName}")
  @Produces("application/geo+json")
  @Operation(summary = "Download a configured GeoJSON boundary")
  public Response getBoundary(@PathParam("areaName") String areaName, @PathParam("boundaryName") String boundaryName) {
    try {
      hasAccess(RESOURCE);
      byte[] content = service().getBoundaryContent(areaName, boundaryName);
      EntityTag tag = TwinResourceEtag.ofBytes(content);
      Response.ResponseBuilder preconditions = baseRequest.evaluatePreconditions(tag);
      if (preconditions != null) return preconditions.tag(tag).build();
      return Response.ok(content).type("application/geo+json").tag(tag).build();
    } catch (TwinConfigurationStore.TwinConfigurationException exception) {
      return status(exception);
    } catch (WebApplicationException exception) {
      return mapAuthOrRethrow(exception);
    } catch (IOException exception) {
      return internalServerError("Unable to read GeoJSON boundary");
    } catch (Exception exception) {
      return internalServerError("Server geospatial administration error");
    }
  }

  @DELETE
  @Path("/areas/{areaName}/boundaries/{boundaryName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Delete a GeoJSON boundary")
  public Response deleteBoundary(@PathParam("areaName") String areaName, @PathParam("boundaryName") String boundaryName) {
    try {
      hasAccess(RESOURCE);
      TwinManagerConfig config = config();
      synchronized (config) {
        GeoSpatialAdminService service = new GeoSpatialAdminService(config);
        GeoSpatialAreaConfigDTO current = service.getArea(areaName).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial area: " + areaName, 404));
        Response precondition = requireIfMatch(current);
        if (precondition != null) return precondition;
        service.deleteBoundary(areaName, boundaryName);
        GeoSpatialAreaConfigDTO updatedArea = service.getArea(areaName).orElseThrow();
        removeUriFromCache(uriInfo.getPath());
        return Response.noContent().tag(TwinResourceEtag.of(updatedArea)).build();
      }
    } catch (TwinConfigurationStore.TwinConfigurationException exception) {
      return status(exception);
    } catch (WebApplicationException exception) {
      return mapAuthOrRethrow(exception);
    } catch (IOException exception) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception exception) {
      return internalServerError("Server geospatial administration error");
    }
  }

  private Response etagResponse(Object entity) {
    EntityTag tag = TwinResourceEtag.of(entity);
    Response.ResponseBuilder preconditions = baseRequest.evaluatePreconditions(tag);
    if (preconditions != null) return preconditions.tag(tag).build();
    return Response.ok(entity).tag(tag).build();
  }

  private Response requireIfMatch(Object current) {
    EntityTag tag = TwinResourceEtag.of(current);
    String ifMatch = request.getHeader("If-Match");
    if (ifMatch == null || ifMatch.isBlank()) {
      return Response.status(PRECONDITION_REQUIRED).entity(new StatusResponse("If-Match is required for this resource mutation")).tag(tag).type(MediaType.APPLICATION_JSON).build();
    }
    Response.ResponseBuilder preconditions = baseRequest.evaluatePreconditions(tag);
    if (preconditions == null) return null;
    return preconditions.entity(new StatusResponse("Resource has changed; reload the current configuration before saving")).tag(tag).type(MediaType.APPLICATION_JSON).build();
  }

  private TwinManagerConfig config() {
    TwinManagerConfig config = TwinManagerConfig.getInstance();
    if (config == null) throw new IllegalStateException("TwinManager configuration is not available");
    return config;
  }

  private GeoSpatialAdminService service() {
    return new GeoSpatialAdminService(config());
  }

  private Response status(TwinConfigurationStore.TwinConfigurationException exception) {
    return Response.status(exception.getStatusCode()).entity(new StatusResponse(exception.getMessage())).type(MediaType.APPLICATION_JSON).build();
  }

  private Response mapAuthOrRethrow(WebApplicationException exception) {
    Response response = exception.getResponse();
    int status = response == null ? 500 : response.getStatus();
    if (status == 401) return Response.status(Response.Status.UNAUTHORIZED).entity(new StatusResponse("Unauthorized")).type(MediaType.APPLICATION_JSON).build();
    if (status == 403) return Response.status(Response.Status.FORBIDDEN).entity(new StatusResponse("Access denied")).type(MediaType.APPLICATION_JSON).build();
    throw exception;
  }
}
