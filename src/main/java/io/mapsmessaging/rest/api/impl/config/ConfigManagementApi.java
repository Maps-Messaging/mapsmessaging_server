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

package io.mapsmessaging.rest.api.impl.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.dto.rest.config.BaseManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.ConfigNamingDTO;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.ConfigurationSchemaDTO;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Server Config Management")
@Path(URI_PATH + "/server/config")
public class ConfigManagementApi extends BaseRestApi {

  private static final String RESOURCE = "server/config";

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List configuration sections",
      description = "Returns the list of known top-level configuration managers/sections.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "List of configuration sections returned",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigNamingDTO[].class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Server configuration error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getConfig() {
    try {
      hasAccess(RESOURCE);

      CacheKey key = new CacheKey(uriInfo.getPath(), "knownManagers");
      ConfigNamingDTO[] managers = getFromCache(key, ConfigNamingDTO[].class);
      if (managers != null) {
        return ok(managers);
      }
      ConfigNamingDTO[] knownManagers = ConfigurationManager.getInstance().getKnownManagers();
      putToCache(key, knownManagers);
      return ok(knownManagers);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server configuration error");
    }
  }

  @GET
  @Path("/{name}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve configuration section value and schema",
      description = "Returns the current configuration section value and its JSON Schema in a single response.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Configuration section returned",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigurationSchemaDTO.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Configuration section not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Server configuration error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getConfigSection(@PathParam("name") String name) {
    try {
      hasAccess(RESOURCE);

      if (name == null || name.isBlank()) {
        return badRequest("name is required");
      }

      CacheKey key = new CacheKey(uriInfo.getPath(), "config+schema");
      ConfigurationSchemaDTO cached = getFromCache(key, ConfigurationSchemaDTO.class);
      if (cached != null) {
        return ok(cached);
      }

      ConfigManager manager = ConfigurationManager.getInstance().getManager(name);
      if (manager == null) {
        return notFound("Unknown configuration section: " + name);
      }

      if (!(manager instanceof BaseManagerConfigDTO baseConfigDto)) {
        return internalServerError("Configuration section is not a DTO: " + name);
      }

      String schemaName = name;
      if (name.contains("Config")) {
        schemaName = name.substring(0, name.indexOf("Config"));
      }

      String schema = ConfigurationManager.getInstance().getSchema(schemaName);
      if (schema == null || schema.isBlank()) {
        return notFound("Schema not found for: " + name);
      }

      JsonObject jsonObject = JsonParser.parseString(schema).getAsJsonObject();
      Map<String, Object> schemaMap = new Gson().fromJson(jsonObject, new TypeToken<Map<String, Object>>() { }.getType());
      ConfigurationSchemaDTO response = new ConfigurationSchemaDTO(baseConfigDto, schemaMap);

      putToCache(key, response);
      return ok(response);
    } catch (WebApplicationException ex) {
      return mapAuthOrRethrow(ex);
    } catch (Exception ex) {
      return internalServerError("Server configuration error");
    }
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
