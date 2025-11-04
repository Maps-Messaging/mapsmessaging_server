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
import jakarta.ws.rs.core.*;

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
  @Path("/server/schema/impl/{schemaId}")
  @Produces(MediaType.WILDCARD)
  @Operation(
      summary = "Get specific schema definition",
      description = "Retrieves the schema artifact bytes by unique ID.",
      responses = {
          @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "*/*")),
          @ApiResponse(responseCode = "304", description = "Not Modified"),
          @ApiResponse(responseCode = "403", description = "Forbidden"),
          @ApiResponse(responseCode = "404", description = "Not Found")
      }
  )
  public Response getSchemaImplById(@PathParam("schemaId") String schemaId, @Context Request request) throws IOException {
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    byte[] body = config.pack().getBytes();
    if (body == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    String mime = resolveSchemaMime(config);

    EntityTag etag = new EntityTag(sha256Hex(body));
    Response.ResponseBuilder precond = (request != null) ? request.evaluatePreconditions(etag) : null;
    if (precond != null) {
      return precond.tag(etag).build(); // 304
    }

    CacheControl cc = new CacheControl();
    if (config.getVersion() > 0) {
      cc.setPrivate(false);
      cc.setMaxAge(31536000); // 1 year
    } else {
      cc.setNoCache(true);
    }

    return Response.ok(body, mime)
        .tag(etag)
        .cacheControl(cc)
        .header("Content-Length", body.length)
        .build();
  }

  private static String resolveSchemaMime(SchemaConfig cfg) {
    if (cfg.getMimeType() != null && !cfg.getMimeType().isEmpty()) {
      return cfg.getMimeType(); // explicit override
    }
    String f = (cfg.getFormat() == null) ? "" : cfg.getFormat().toLowerCase(java.util.Locale.ROOT);
    return switch (f) {
      case "json"       -> "application/schema+json";   // JSON Schema
      case "protobuf"   -> "application/x-protobuf";    // FileDescriptorSet (binary)
      case "avro"       -> "application/avro+json";     // AVSC JSON
      case "xml"        -> "application/xml";           // XSD
      case "cbor"       -> "application/cddl";          // CDDL text for CBOR
      case "messagepack", "msgpack" -> "application/schema+json"; // JSON Schema / JTD
      case "csv"        -> "application/schema+json";   // schema describing CSV
      case "native", "raw" -> "application/octet-stream";
      default           -> "application/octet-stream";
    };
  }


  private static String sha256Hex(byte[] bytes) {
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(bytes);
      return java.util.HexFormat.of().formatHex(digest);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
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
