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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.ConfigurationSchemaDTO;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = String[].class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public String[] getConfig() throws ExecutionException, InterruptedException, TimeoutException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "knownManagers");
    String[] managers = getFromCache(key, String[].class);
    if (managers != null) {
      return managers;
    }
    managers = ConfigurationManager.getInstance().getKnownManagers();
    putToCache(key, managers);
    return managers;
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
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ConfigurationSchemaDTO.class)
              )
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Configuration section not found"),
          @ApiResponse(responseCode = "500", description = "Server configuration error")
      }
  )
  public ConfigurationSchemaDTO getConfigSection(
      @PathParam("name") String name
  ) throws ExecutionException, InterruptedException, TimeoutException {

    hasAccess(RESOURCE);

    if (name == null || name.isBlank()) {
      throw new WebApplicationException("name is required", Response.Status.BAD_REQUEST);
    }

    CacheKey key = new CacheKey(uriInfo.getPath(), "config+schema");
    ConfigurationSchemaDTO cached = getFromCache(key, ConfigurationSchemaDTO.class);
    if (cached != null) {
      return cached;
    }

    ConfigManager manager = ConfigurationManager.getInstance().getManager(name);
    if (manager == null) {
      throw new WebApplicationException("Unknown configuration section: " + name, Response.Status.NOT_FOUND);
    }

    if (!(manager instanceof BaseConfigDTO dto)) {
      throw new WebApplicationException(
          "Configuration section is not a DTO: " + name,
          Response.Status.INTERNAL_SERVER_ERROR
      );
    }
    String schemaName = name;
    if(name.contains("Config")){
      schemaName = name.substring(0, name.indexOf("Config"));
    }
    String schema = ConfigurationManager.getInstance().getSchema(schemaName);
    if (schema == null || schema.isBlank()) {
      throw new WebApplicationException("Schema not found for: " + name, Response.Status.NOT_FOUND);
    }
    System.err.println("Schema for " + name + ": " + schema);
    JsonObject jsonObject = JsonParser.parseString(schema).getAsJsonObject();
    ConfigurationSchemaDTO response = new ConfigurationSchemaDTO(dto, jsonObject);
    putToCache(key, response);
    return response;
  }
}
