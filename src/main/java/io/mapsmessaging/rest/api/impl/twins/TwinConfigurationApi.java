/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.rest.api.impl.twins;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.state.config.DroneInfo;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.TwinPublishConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
import java.util.Map;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(
    name = "Server Twin Configuration",
    description = "Configuration endpoints for digital twin integrations. Changes are persisted to TwinManager configuration and do not reload running state services."
)
@Path(URI_PATH + "/server/twin/config")
public class TwinConfigurationApi extends BaseRestApi {

  private static final String RESOURCE = "server/twin/config";

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get twin configuration",
      description = "Returns the current persisted twin manager configuration, including core timing, publishing, TAK, N2K, MAVLink, drone metadata and state adapter sections.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Twin configuration returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TwinManagerConfig.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getTwinConfiguration() {
    try {
      hasAccess(RESOURCE);
      return ok(store().getConfig());
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @GET
  @Path("/core")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get core twin manager configuration",
      description = "Returns persisted lifecycle timing and root path settings. Runtime twin manager timing is not reloaded by this endpoint.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Core twin configuration returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TwinCoreConfigDTO.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getCoreConfig() {
    try {
      hasAccess(RESOURCE);
      return ok(store().getCoreConfig());
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @PUT
  @Path("/core")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update core twin manager configuration",
      description = "Persists core twin lifecycle timing and root path settings. The running StateManagerAgent is not reloaded.",
      requestBody = @RequestBody(
          description = "Core twin manager timing and lifecycle configuration to persist",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = TwinCoreConfigDTO.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "Core twin configuration persisted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TwinCoreConfigDTO.class))),
          @ApiResponse(responseCode = "400", description = "Invalid core twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response updateCoreConfig(TwinCoreConfigDTO coreConfig) {
    try {
      hasAccess(RESOURCE);
      store().updateCoreConfig(coreConfig);
      removeUriFromCache(uriInfo.getPath());
      return ok(coreConfig);
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @GET
  @Path("/tak")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get TAK twin configuration",
      description = "Returns the persisted TAK publishing configuration used by twin state integration. Runtime TAK connections are not reloaded by this endpoint.",
      responses = {
          @ApiResponse(responseCode = "200", description = "TAK configuration returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TakProtocolDTO.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "TAK configuration is not configured", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getTakConfig() {
    try {
      hasAccess(RESOURCE);
      return optionalResponse(store().getTakConfig(), "TAK configuration is not configured");
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @PUT
  @Path("/tak")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create or update TAK twin configuration",
      description = "Persists TAK configuration for the twin manager. Existing runtime TAK observers are not restarted.",
      requestBody = @RequestBody(
          description = "TAK protocol configuration to persist",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = TakProtocolDTO.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "TAK configuration persisted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TakProtocolDTO.class))),
          @ApiResponse(responseCode = "400", description = "Invalid TAK configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response putTakConfig(TakProtocolDTO takConfig) {
    return mutateOk(takConfig, () -> store().putTakConfig(takConfig));
  }

  @DELETE
  @Path("/tak")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete TAK twin configuration",
      description = "Removes the persisted TAK configuration. Existing runtime TAK observers are not stopped.",
      responses = {
          @ApiResponse(responseCode = "204", description = "TAK configuration removed"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "TAK configuration is not configured", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response deleteTakConfig() {
    return mutateNoContent(() -> store().deleteTakConfig());
  }

  @GET
  @Path("/publish")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get twin publish configuration",
      description = "Returns persisted configuration for publishing twin updates to messaging topics. Runtime publishers are not reloaded by this endpoint.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Publish configuration returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TwinPublishConfigDTO.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "Publish configuration is not configured", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getPublishConfig() {
    try {
      hasAccess(RESOURCE);
      return optionalResponse(store().getPublishConfig(), "Publish configuration is not configured");
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @PUT
  @Path("/publish")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create or update twin publish configuration",
      description = "Persists publishing configuration for twin updates. The running TwinPublisherManager is not restarted.",
      requestBody = @RequestBody(
          description = "Twin publish configuration to persist",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = TwinPublishConfigDTO.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "Publish configuration persisted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TwinPublishConfigDTO.class))),
          @ApiResponse(responseCode = "400", description = "Invalid publish configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response putPublishConfig(TwinPublishConfigDTO publishConfig) {
    return mutateOk(publishConfig, () -> store().putPublishConfig(publishConfig));
  }

  @DELETE
  @Path("/publish")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete twin publish configuration",
      description = "Removes the persisted publish configuration. Existing runtime publishers are not stopped.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Publish configuration removed"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "Publish configuration is not configured", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response deletePublishConfig() {
    return mutateNoContent(() -> store().deletePublishConfig());
  }

  @GET
  @Path("/n2k")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get N2K twin configuration",
      description = "Returns persisted NMEA 2000 twin integration configuration. Runtime N2K subscriptions and AIS projection are not reloaded by this endpoint.",
      responses = {
          @ApiResponse(responseCode = "200", description = "N2K configuration returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = N2KTwinConfig.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "N2K configuration is not configured", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getN2kConfig() {
    try {
      hasAccess(RESOURCE);
      return optionalResponse(store().getN2kConfig(), "N2K configuration is not configured");
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @PUT
  @Path("/n2k")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create or update N2K twin configuration",
      description = "Persists NMEA 2000 twin integration configuration. The running N2K session and AIS manager are not restarted.",
      requestBody = @RequestBody(
          description = "N2K twin integration configuration to persist",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = N2KTwinConfig.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "N2K configuration persisted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = N2KTwinConfig.class))),
          @ApiResponse(responseCode = "400", description = "Invalid N2K configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response putN2kConfig(N2KTwinConfig n2kConfig) {
    return mutateOk(n2kConfig, () -> store().putN2kConfig(n2kConfig));
  }

  @DELETE
  @Path("/n2k")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Disable N2K twin configuration",
      description = "Persists N2K as disabled. Existing runtime N2K subscriptions and AIS monitors are not stopped.",
      responses = {
          @ApiResponse(responseCode = "204", description = "N2K configuration disabled"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "N2K configuration is not configured", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response deleteN2kConfig() {
    return mutateNoContent(() -> store().deleteN2kConfig());
  }

  @GET
  @Path("/mavlink")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List MAVLink twin source configurations",
      description = "Returns persisted MAVLink topic sources that are processed into twin state. Runtime MAVLink subscribers are not reloaded by this endpoint.",
      responses = {
          @ApiResponse(responseCode = "200", description = "MAVLink twin sources returned", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MavlinkTwinConfigDTO.class)))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response listMavlinkSources() {
    try {
      hasAccess(RESOURCE);
      return ok(store().listMavlinkSources().toArray(new MavlinkTwinConfigDTO[0]));
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @GET
  @Path("/mavlink/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get MAVLink twin source configuration",
      description = "Returns one persisted MAVLink twin source configuration by name.",
      responses = {
          @ApiResponse(responseCode = "200", description = "MAVLink twin source returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MavlinkTwinConfigDTO.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "MAVLink twin source not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getMavlinkSource(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      return store().getMavlinkSource(name)
          .map(this::ok)
          .orElseGet(() -> notFound("Unknown MAVLink twin source: " + name));
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @POST
  @Path("/mavlink")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create MAVLink twin source configuration",
      description = "Adds a persisted MAVLink topic source. The running MavlinkTwinManager is not reloaded.",
      requestBody = @RequestBody(
          description = "MAVLink twin source configuration to add",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = MavlinkTwinConfigDTO.class))
      ),
      responses = {
          @ApiResponse(responseCode = "201", description = "MAVLink twin source created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MavlinkTwinConfigDTO.class))),
          @ApiResponse(responseCode = "400", description = "Invalid MAVLink twin source configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "409", description = "MAVLink twin source already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response createMavlinkSource(MavlinkTwinConfigDTO mavlinkConfig) {
    try {
      hasAccess(RESOURCE);
      store().createMavlinkSource(mavlinkConfig);
      removeUriFromCache(uriInfo.getPath());
      return Response.status(Response.Status.CREATED).entity(mavlinkConfig).build();
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @PUT
  @Path("/mavlink/{name}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update MAVLink twin source configuration",
      description = "Replaces a persisted MAVLink topic source. The path name must match the body name. Runtime subscribers are not reloaded.",
      requestBody = @RequestBody(
          description = "MAVLink twin source configuration to persist",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = MavlinkTwinConfigDTO.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "MAVLink twin source updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MavlinkTwinConfigDTO.class))),
          @ApiResponse(responseCode = "400", description = "Invalid MAVLink twin source configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "MAVLink twin source not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response updateMavlinkSource(@PathParam("name") String name, MavlinkTwinConfigDTO mavlinkConfig) {
    try {
      hasAccess(RESOURCE);
      store().updateMavlinkSource(name, mavlinkConfig);
      removeUriFromCache(uriInfo.getPath());
      return ok(mavlinkConfig);
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @DELETE
  @Path("/mavlink/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete MAVLink twin source configuration",
      description = "Removes a persisted MAVLink topic source. Runtime subscribers are not stopped.",
      responses = {
          @ApiResponse(responseCode = "204", description = "MAVLink twin source deleted"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "MAVLink twin source not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response deleteMavlinkSource(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      store().deleteMavlinkSource(name);
      removeUriFromCache(uriInfo.getPath());
      return noContent();
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @GET
  @Path("/drone-info")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List known drone configurations",
      description = "Returns persisted drone metadata used when MAVLink sources create or update drone twins. Runtime drone metadata registries are not reloaded.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Drone configurations returned", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DroneInfo.class)))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response listDrones() {
    try {
      hasAccess(RESOURCE);
      return ok(store().listDrones().toArray(new DroneInfo[0]));
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @GET
  @Path("/drone-info/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get known drone configuration",
      description = "Returns one persisted drone metadata entry by name.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Drone configuration returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DroneInfo.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "Drone configuration not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getDrone(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      return store().getDrone(name)
          .map(this::ok)
          .orElseGet(() -> notFound("Unknown drone configuration: " + name));
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @POST
  @Path("/drone-info")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create known drone configuration",
      description = "Adds persisted drone metadata. Existing runtime drone metadata registries are not reloaded.",
      requestBody = @RequestBody(
          description = "Drone metadata configuration to add",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = DroneInfo.class))
      ),
      responses = {
          @ApiResponse(responseCode = "201", description = "Drone configuration created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DroneInfo.class))),
          @ApiResponse(responseCode = "400", description = "Invalid drone configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "409", description = "Drone configuration already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response createDrone(DroneInfo droneInfo) {
    try {
      hasAccess(RESOURCE);
      store().createDrone(droneInfo);
      removeUriFromCache(uriInfo.getPath());
      return Response.status(Response.Status.CREATED).entity(droneInfo).build();
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @PUT
  @Path("/drone-info/{name}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update known drone configuration",
      description = "Replaces persisted drone metadata. The path name must match the body name. Runtime drone metadata registries are not reloaded.",
      requestBody = @RequestBody(
          description = "Drone metadata configuration to persist",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = DroneInfo.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "Drone configuration updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DroneInfo.class))),
          @ApiResponse(responseCode = "400", description = "Invalid drone configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "Drone configuration not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response updateDrone(@PathParam("name") String name, DroneInfo droneInfo) {
    try {
      hasAccess(RESOURCE);
      store().updateDrone(name, droneInfo);
      removeUriFromCache(uriInfo.getPath());
      return ok(droneInfo);
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @DELETE
  @Path("/drone-info/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete known drone configuration",
      description = "Removes persisted drone metadata. Runtime drone metadata registries are not reloaded.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Drone configuration deleted"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "Drone configuration not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response deleteDrone(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      store().deleteDrone(name);
      removeUriFromCache(uriInfo.getPath());
      return noContent();
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @GET
  @Path("/adapters")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List state adapter configurations",
      description = "Returns persisted state adapter configuration blocks keyed by adapter name. Runtime state adapter instances are not reloaded.",
      responses = {
          @ApiResponse(responseCode = "200", description = "State adapter configurations returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response listAdapterConfigs() {
    try {
      hasAccess(RESOURCE);
      return ok(store().listAdapterConfigs());
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @PUT
  @Path("/adapters")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Replace all state adapter configurations",
      description = "Replaces the persisted state adapter configuration map. Runtime adapter instances are not reloaded.",
      requestBody = @RequestBody(
          description = "Map of adapter names to adapter-specific configuration blocks",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "State adapter configuration map persisted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
          @ApiResponse(responseCode = "400", description = "Invalid adapter configuration map", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response replaceAdapterConfigs(Map<String, ConfigurationProperties> adapterConfigs) {
    return mutateOk(adapterConfigs, () -> store().replaceAdapterConfigs(adapterConfigs));
  }

  @GET
  @Path("/adapters/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get state adapter configuration",
      description = "Returns one persisted state adapter configuration block by adapter name.",
      responses = {
          @ApiResponse(responseCode = "200", description = "State adapter configuration returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigurationProperties.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "State adapter configuration not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Server twin configuration error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getAdapterConfig(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);
      return optionalResponse(store().getAdapterConfig(name), "Unknown state adapter configuration: " + name);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  @POST
  @Path("/adapters/{name}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Create state adapter configuration",
      description = "Adds a persisted adapter-specific configuration block. Runtime state adapter instances are not reloaded.",
      requestBody = @RequestBody(
          description = "Adapter-specific configuration block to add",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigurationProperties.class))
      ),
      responses = {
          @ApiResponse(responseCode = "201", description = "State adapter configuration created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigurationProperties.class))),
          @ApiResponse(responseCode = "400", description = "Invalid adapter configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "409", description = "State adapter configuration already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response createAdapterConfig(@PathParam("name") String name, ConfigurationProperties adapterConfig) {
    return mutateCreated(adapterConfig, () -> store().createAdapterConfig(name, adapterConfig));
  }

  @PUT
  @Path("/adapters/{name}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update state adapter configuration",
      description = "Replaces one persisted adapter-specific configuration block. Runtime state adapter instances are not reloaded.",
      requestBody = @RequestBody(
          description = "Adapter-specific configuration block to persist",
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigurationProperties.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "State adapter configuration updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigurationProperties.class))),
          @ApiResponse(responseCode = "400", description = "Invalid adapter configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "State adapter configuration not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response updateAdapterConfig(@PathParam("name") String name, ConfigurationProperties adapterConfig) {
    return mutateOk(adapterConfig, () -> store().updateAdapterConfig(name, adapterConfig));
  }

  @DELETE
  @Path("/adapters/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete state adapter configuration",
      description = "Removes one persisted state adapter configuration block. Runtime state adapter instances are not stopped.",
      responses = {
          @ApiResponse(responseCode = "204", description = "State adapter configuration deleted"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "404", description = "State adapter configuration not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "500", description = "Unable to save twin configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response deleteAdapterConfig(@PathParam("name") String name) {
    return mutateNoContent(() -> store().deleteAdapterConfig(name));
  }

  TwinConfigurationStore store() {
    TwinManagerConfig config = TwinManagerConfig.getInstance();
    if (config == null) {
      throw new IllegalStateException("TwinManager configuration is not available");
    }
    return new TwinConfigurationStore(config);
  }

  private Response status(TwinConfigurationStore.TwinConfigurationException exception) {
    return Response.status(exception.getStatusCode())
        .entity(new StatusResponse(exception.getMessage()))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private <T> Response optionalResponse(java.util.Optional<T> optional, String message) {
    return optional
        .map(this::ok)
        .orElseGet(() -> notFound(message));
  }

  private Response mutateCreated(Object entity, SaveAction action) {
    try {
      hasAccess(RESOURCE);
      action.run();
      removeUriFromCache(uriInfo.getPath());
      return Response.status(Response.Status.CREATED).entity(entity).build();
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
    }
  }

  private Response mutateOk(Object entity, SaveAction action) {
    try {
      hasAccess(RESOURCE);
      action.run();
      removeUriFromCache(uriInfo.getPath());
      return ok(entity);
    } catch (TwinConfigurationStore.TwinConfigurationException ex) {
      return status(ex);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (IOException ex) {
      return internalServerError("Unable to save twin configuration");
    } catch (Exception ex) {
      return internalServerError("Server twin configuration error");
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
      return internalServerError("Server twin configuration error");
    }
  }

  @FunctionalInterface
  private interface SaveAction {

    void run() throws IOException;
  }

  private Response mapAuthOrRethrow(WebApplicationException exception) {
    Response response = exception.getResponse();
    int status = response == null ? 500 : response.getStatus();
    if (status == 401) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new StatusResponse("Unauthorized"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (status == 403) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity(new StatusResponse("Access denied"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    throw exception;
  }
}
