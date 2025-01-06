/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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
 */

package io.mapsmessaging.rest.api.impl.schema;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.dto.rest.schema.SchemaPostDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.SchemaConfigResponse;
import io.mapsmessaging.rest.responses.SchemaMapResponse;
import io.mapsmessaging.rest.responses.SchemaResponse;
import io.mapsmessaging.rest.responses.StringListResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Tag(name = "Schema Management")
@Path(URI_PATH)
public class SchemaQueryApi extends BaseRestApi {

  private static final String RESOURCE = "schemas";

  @DELETE
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Delete specific schema", description = "Delete the schema configuration by unique id")
  @ApiResponse(responseCode = "200", description = "Schema deleted (or does not exist)")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public BaseResponse deleteSchemaById(@PathParam("schemaId") String schemaId) {
    hasAccess(RESOURCE);
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config != null) {
      SchemaManager.getInstance().removeSchema(schemaId);
    }
    return new BaseResponse();
  }

  @DELETE
  @Path("/server/schema")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Delete all schemas", description = "Deletes all the schema configurations (optionally filtered)")
  @ApiResponse(responseCode = "200", description = "Schemas deleted")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public BaseResponse deleteAllSchemas(@QueryParam("filter") String filter) throws ParseException {
    hasAccess(RESOURCE);
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<SchemaConfig> result =
        SchemaManager.getInstance().getAll().stream()
            .filter(protocol -> parser == null || parser.evaluate(protocol))
            .collect(Collectors.toList());
    for (SchemaConfig schema : result) {
      SchemaManager.getInstance().removeSchema(schema.getUniqueId());
    }
    return new BaseResponse();
  }

  @POST
  @Path("/server/schema")
  @Consumes({MediaType.APPLICATION_JSON})
  @Operation(summary = "Add new schema", description = "Adds a new schema to the registry")
  @ApiResponse(responseCode = "200", description = "Schema added")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public BaseResponse addSchema(SchemaPostDTO jsonString) throws IOException {
    hasAccess(RESOURCE);
    SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(jsonString.getSchema());
    SchemaManager.getInstance().addSchema(jsonString.getContext(), config);
    return new BaseResponse();
  }

  @GET
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get schema", description = "Returns a specific schema by its unique ID")
  @ApiResponse(responseCode = "200", description = "Schema returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Schema not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public SchemaResponse getSchemaById(@PathParam("schemaId") String schemaId) throws IOException {
    hasAccess(RESOURCE);
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config != null) {
      return new SchemaResponse(config.pack());
    }
    throw new WebApplicationException("Schema not found", 404);
  }

  @GET
  @Path("/server/schema/context/{context}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get schema by context", description = "Returns all schemas matching the provided context")
  @ApiResponse(responseCode = "200", description = "Schemas returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
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
  @Operation(summary = "Get schema by type", description = "Returns all schemas matching the provided type")
  @ApiResponse(responseCode = "200", description = "Schemas returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
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
  @Operation(summary = "Get all schemas", description = "Returns all schemas, optionally filtered")
  @ApiResponse(responseCode = "200", description = "List of schemas returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public SchemaConfigResponse getAllSchemas(@QueryParam("filter") String filter) throws ParseException {
    hasAccess(RESOURCE);
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<SchemaConfig> result =
        SchemaManager.getInstance().getAll().stream()
            .filter(protocol -> parser == null || parser.evaluate(protocol))
            .collect(Collectors.toList());
    return new SchemaConfigResponse(result);
  }

  @GET
  @Path("/server/schema/map")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get schemas and their configuration mappings", description = "Returns mapping of context to schema IDs")
  @ApiResponse(responseCode = "200", description = "Mapping returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
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
  @Operation(summary = "Get all known formats", description = "Returns a list of supported message formats")
  @ApiResponse(responseCode = "200", description = "Formats returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public StringListResponse getKnownFormats() {
    hasAccess(RESOURCE);
    return new StringListResponse(SchemaManager.getInstance().getMessageFormats());
  }

  @GET
  @Path("/server/schema/link-format")
  @Produces({MediaType.TEXT_PLAIN})
  @Operation(summary = "Get link-format config", description = "Returns the link-format list")
  @ApiResponse(responseCode = "200", description = "Link-format returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public String getLinkFormat() {
    hasAccess(RESOURCE);
    return SchemaManager.getInstance().buildLinkFormatResponse();
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
