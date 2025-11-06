/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.rest.api.impl.schema;

import io.mapsmessaging.dto.rest.schema.SchemaPostDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.*;
import io.mapsmessaging.rest.responses.SchemaImplementationResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Schema Management", description = "Endpoints for managing and querying schemas in the system.")
@Path(URI_PATH)
public class SchemaQueryApi extends BaseRestApi {

  private static final String RESOURCE = "schemas";

  @DELETE
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete specific schema",
      description = "Deletes a schema configuration by its unique ID.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Schema not found"),
      }
  )
  public StatusResponse deleteSchemaById(@PathParam("schemaId") String schemaId) {
    hasAccess(RESOURCE);
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config != null) {
      SchemaManager.getInstance().removeSchema(schemaId);
      return new StatusResponse("Success");
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return new StatusResponse("Failure");
  }

  @DELETE
  @Path("/server/schema")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete all schemas",
      description = "Deletes all schemas, optionally filtered by a query string.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse deleteAllSchemas(@QueryParam("filter") String filter) throws ParseException {
    hasAccess(RESOURCE);
    ParserExecutor parser =
        (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<SchemaConfig> result =
        SchemaManager.getInstance().getAll().stream()
            .filter(protocol -> parser == null || parser.evaluate(protocol))
            .collect(Collectors.toList());
    for (SchemaConfig schema : result) {
      SchemaManager.getInstance().removeSchema(schema.getUniqueId());
    }
    return new StatusResponse("Success");
  }

  @POST
  @Path("/server/schema")
  @Consumes({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add new schema",
      description = "Adds a new schema configuration to the system.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse addSchema(SchemaPostDTO jsonString) throws IOException {
    hasAccess(RESOURCE);
    SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(jsonString.getSchema());
    SchemaManager.getInstance().addSchema(jsonString.getContext(), config);
    return new StatusResponse("Success");
  }

  @GET
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get specific schema",
      description = "Retrieves the details of a specific schema by its unique ID.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public SchemaResponse getSchemaById(@PathParam("schemaId") String schemaId) throws IOException {
    hasAccess(RESOURCE);
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config != null) {
      return new SchemaResponse(config.pack());
    }
    return new SchemaResponse(new ArrayList<>());
  }

  @GET
  @Path("/server/schema/context/{context}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get schemas by context",
      description = "Retrieves all schemas that match the specified context.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public SchemaResponse getSchemaByContext(@PathParam("context") String context) throws IOException {
    hasAccess(RESOURCE);
    List<SchemaConfig> config = SchemaManager.getInstance().getSchemaByContext(context);
    if (config != null) {
      return new SchemaResponse(convert(config));
    }
    return new SchemaResponse(new ArrayList<>());
  }

  @GET
  @Path("/server/schema/type/{type}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get schemas by type",
      description = "Retrieves all schemas that match the specified type.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public SchemaResponse getSchemaByType(@PathParam("type") String type) throws IOException {
    hasAccess(RESOURCE);
    List<SchemaConfig> config = SchemaManager.getInstance().getSchemas(type);
    if (config != null) {
      return new SchemaResponse(convert(config));
    }
    return new SchemaResponse(new ArrayList<>());
  }

  @GET
  @Path("/server/schema")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all schemas",
      description = "Retrieves all schema configurations, optionally filtered by a query string.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaConfigResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public SchemaConfigResponse getAllSchemas(@QueryParam("filter") String filter) throws ParseException {
    hasAccess(RESOURCE);
    ParserExecutor parser =
        (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<SchemaConfig> result =
        SchemaManager.getInstance().getAll().stream()
            .filter(protocol -> parser == null || parser.evaluate(protocol))
            .collect(Collectors.toList());
    return new SchemaConfigResponse(result);
  }

  @GET
  @Path("/server/schema/map")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get schema mappings",
      description = "Retrieves all schemas and their associated mapping information.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaMapResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public SchemaMapResponse getSchemaMapping() {
    hasAccess(RESOURCE);
    Map<String, List<SchemaConfig>> map = SchemaManager.getInstance().getMappedSchemas();
    Map<String, List<String>> responseMap = new LinkedHashMap<>();
    for (Entry<String, List<SchemaConfig>> entry : map.entrySet()) {
      responseMap.put(entry.getKey(), convertToId(entry.getValue()));
    }
    return new SchemaMapResponse(responseMap);
  }

  @GET
  @Path("/server/schema/formats")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get supported formats",
      description = "Retrieves a list of all known schema formats supported by the system.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StringListResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StringListResponse getKnownFormats() {
    hasAccess(RESOURCE);
    return new StringListResponse(SchemaManager.getInstance().getMessageFormats());
  }

  @GET
  @Path("/server/schema/link-format")
  @Produces({MediaType.TEXT_PLAIN})
  @Operation(
      summary = "Get link-format configuration",
      description = "Retrieves the link-format configuration list.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public String getLinkFormat() {
    hasAccess(RESOURCE);
    return SchemaManager.getInstance().buildLinkFormatResponse();
  }

  @GET
  @Path("/server/schema/impl/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get schema implementation details",
      description = "Retrieves implementation details for a specific schema, including formatter information and capabilities.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaImplementationResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Schema not found"),
      }
  )
  public SchemaImplementationResponse getSchemaImplementation(@PathParam("schemaId") String schemaId) throws IOException {
    hasAccess(RESOURCE);
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config != null) {
      SchemaImplementationResponse response = new SchemaImplementationResponse();
      response.setSchemaId(schemaId);
      response.setSchemaType(config.getClass().getSimpleName());
      response.setSchemaName(config.getName());
      response.setSchemaVersion(config.getVersion());
      response.setInterfaceDescription(config.getInterfaceDescription());
      response.setResourceType(config.getResourceType());
      
      // Get formatter information if available
      try {
        var formatter = SchemaManager.getInstance().getMessageFormatter(schemaId);
        if (formatter != null) {
          response.setFormatterType(formatter.getClass().getSimpleName());
          response.setFormatterAvailable(true);
        } else {
          response.setFormatterAvailable(false);
        }
      } catch (Exception e) {
        response.setFormatterAvailable(false);
        response.setFormatterError(e.getMessage());
      }
      
      return response;
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return new SchemaImplementationResponse();
  }

  private List<String> convert(List<SchemaConfig> configs) throws IOException {
    List<String> data = new ArrayList<>();
    for (SchemaConfig config : configs) {
      data.add(config.pack());
    }
    return data;
  }

  private List<String> convertToId(List<SchemaConfig> configs) {
    List<String> data = new ArrayList<>();
    for (SchemaConfig config : configs) {
      data.add(config.getUniqueId());
    }
    return data;
  }
}
