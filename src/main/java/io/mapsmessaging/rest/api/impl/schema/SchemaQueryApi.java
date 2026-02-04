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

package io.mapsmessaging.rest.api.impl.schema;

import io.mapsmessaging.dto.helpers.SchemaConfigDtoMapper;
import io.mapsmessaging.dto.rest.schema.SchemaConfigDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.SchemaMapResponse;
import io.mapsmessaging.rest.responses.SchemaPostDTO;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.StringListResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.config.SchemaResource;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Schema Management", description = "Endpoints for managing and querying schemas in the system.")
@Path(URI_PATH + "/server/schemas")
public class SchemaQueryApi extends BaseRestApi {

  private static final String RESOURCE = "schemas";

  @DELETE
  @Path("/{schemaId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete specific schema",
      description = "Deletes a schema configuration by its unique ID.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "404",
              description = "Schema not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response deleteSchemaById(@PathParam("schemaId") String schemaId) {
    hasAccess(RESOURCE);

    if (schemaId == null || schemaId.trim().isEmpty()) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("schemaId is required"))
          .build();
    }

    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config == null) {
      return Response.status(Status.NOT_FOUND)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Schema not found: " + schemaId))
          .build();
    }

    SchemaManager.getInstance().removeSchema(schemaId);
    return Response.ok(new StatusResponse("Success"), MediaType.APPLICATION_JSON).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete all schemas",
      description = "Deletes all schemas, optionally filtered by a query string.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public Response deleteAllSchemas(@QueryParam("filter") String filter) {
    hasAccess(RESOURCE);

    ParserExecutor parser;
    try {
      parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    } catch (ParseException exception) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Invalid filter: " + exception.getMessage()))
          .build();
    }

    List<SchemaConfig> schemasToDelete =
        SchemaManager.getInstance().getAll().stream()
            .filter(schemaConfig -> parser == null || parser.evaluate(schemaConfig))
            .toList();

    for (SchemaConfig schemaConfig : schemasToDelete) {
      SchemaManager.getInstance().removeSchema(schemaConfig.getUniqueId());
    }

    return Response.ok("Success", MediaType.APPLICATION_JSON).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add new schema",
      description = "Adds a new schema configuration to the system.",
      requestBody = @RequestBody(
          required = true,
          description = "Schema post payload",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaPostDTO.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaConfigDTO.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response addSchema(SchemaPostDTO schemaPostDTO) {
    hasAccess(RESOURCE);

    if (schemaPostDTO == null) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Request body is required"))
          .build();
    }

    String context = schemaPostDTO.getContext();
    if (context == null || context.trim().isEmpty()) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("context is required"))
          .build();
    }

    String schema = schemaPostDTO.getSchema();
    if (schema == null || schema.trim().isEmpty()) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("schema is required"))
          .build();
    }

    SchemaConfig config;
    try {
      config = SchemaConfigFactory.getInstance().constructConfig(schema);
    } catch (IllegalArgumentException|IOException exception) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Invalid schema: " + exception.getMessage()))
          .build();
    } catch (RuntimeException exception) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failed to add schema: " + exception.getMessage()))
          .build();
    }

    SchemaManager.getInstance().addSchema(context, config);
    return Response.ok(SchemaConfigDtoMapper.toDto(config), MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/{schemaId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get specific schema",
      description = "Retrieves the details of a specific schema by its unique ID.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaConfigDTO.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "404",
              description = "Schema not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getSchemaById(@PathParam("schemaId") String schemaId) {
    hasAccess(RESOURCE);

    if (schemaId == null || schemaId.trim().isEmpty()) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("schemaId is required"))
          .build();
    }

    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config == null) {
      return Response.status(Status.NOT_FOUND)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Schema not found: " + schemaId))
          .build();
    }
    return Response.ok(SchemaConfigDtoMapper.toDto(config), MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/context/{context}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get schemas by context",
      description = "Retrieves all schemas that match the specified context.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = String.class))
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getSchemaByContext(@PathParam("context") String context) {
    hasAccess(RESOURCE);

    String decodedContext;
    try {
      decodedContext = java.net.URLDecoder.decode(context, java.nio.charset.StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Invalid context encoding"))
          .build();
    }

    if (decodedContext == null || decodedContext.trim().isEmpty()) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("context is required"))
          .build();
    }

    List<SchemaResource> schemaResources = SchemaManager.getInstance().getSchemaByContext(decodedContext);
    if (schemaResources == null || schemaResources.isEmpty()) {
      return Response.ok(new String[0], MediaType.APPLICATION_JSON).build();
    }

    List<String> packedSchemas;
    try {
      packedSchemas = convert(schemaResources);
    } catch (IOException exception) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failed to read schemas: " + exception.getMessage()))
          .build();
    }

    return Response.ok(packedSchemas.toArray(new String[0]), MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/type/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get schemas by type",
      description = "Retrieves all schemas that match the specified type.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = String.class))
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getSchemaByType(@PathParam("type") String type) {
    hasAccess(RESOURCE);
    String decodedType;
    try {
      decodedType = java.net.URLDecoder.decode(type, java.nio.charset.StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Invalid context encoding"))
          .build();
    }

    if (decodedType == null || decodedType.trim().isEmpty()) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("type is required"))
          .build();
    }

    List<SchemaResource> schemaResources = SchemaManager.getInstance().getSchemas(decodedType);
    if (schemaResources == null) {
      return Response.ok(new String[0], MediaType.APPLICATION_JSON).build();
    }

    List<String> packedSchemas;
    try {
      packedSchemas = convert(schemaResources);
    } catch (IOException exception) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failed to read schemas: " + exception.getMessage()))
          .build();
    }

    return Response.ok(packedSchemas.toArray(new String[0]), MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all schemas",
      description = "Retrieves all schema configurations, optionally filtered by a query string.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = SchemaConfigDTO.class))
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public Response getAllSchemas(@QueryParam("filter") String filter) {
    hasAccess(RESOURCE);

    ParserExecutor parser;
    try {
      parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    } catch (ParseException exception) {
      return Response.status(Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Invalid filter: " + exception.getMessage()))
          .build();
    }

    List<SchemaConfig> schemas =
        SchemaManager.getInstance().getAll().stream()
            .filter(schemaConfig -> parser == null || parser.evaluate(schemaConfig))
            .toList();

    List<SchemaConfigDTO> schemaList = new ArrayList<>();
    for(SchemaConfig schemaConfig : schemas) {
      schemaList.add(SchemaConfigDtoMapper.toDto(schemaConfig));
    }
    return Response.ok(schemaList.toArray(new SchemaConfigDTO[0]), MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/map")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get schema mappings",
      description = "Retrieves all schemas and their associated mapping information.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = SchemaMapResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
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
  @Path("/impl/{schemaId}")
  @Produces(MediaType.WILDCARD)
  @Operation(
      summary = "Get specific schema definition",
      description = "Retrieves the schema artifact bytes by unique ID.",
      responses = {
          @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "*/*")),
          @ApiResponse(responseCode = "304", description = "Not Modified"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "Forbidden"),
          @ApiResponse(responseCode = "404", description = "Not Found")
      }
  )
  public Response getSchemaImplById(@PathParam("schemaId") String schemaId, Request request) {
    hasAccess(RESOURCE);

    if (schemaId == null || schemaId.trim().isEmpty()) {
      return Response.status(Status.NOT_FOUND).build();
    }

    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    byte[] body;
    try {
      body = config.packAsBytes();
    } catch (IOException exception) {
      return Response.status(Status.NOT_FOUND).build();
    }

    if (body == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    String mime = resolveSchemaMime(config);

    EntityTag entityTag = new EntityTag(sha256Hex(body));
    Response.ResponseBuilder preconditions = (request != null) ? request.evaluatePreconditions(entityTag) : null;
    if (preconditions != null) {
      return preconditions.tag(entityTag).build();
    }

    CacheControl cacheControl = new CacheControl();
    if (config.getVersion() != null && !config.getVersion().isEmpty()) {
      cacheControl.setPrivate(false);
      cacheControl.setMaxAge(31536000);
    } else {
      cacheControl.setNoCache(true);
    }

    return Response.ok(body, mime)
        .tag(entityTag)
        .cacheControl(cacheControl)
        .header("Content-Length", body.length)
        .build();
  }

  @GET
  @Path("/formats")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get supported formats",
      description = "Retrieves a list of all known schema formats supported by the system.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StringListResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public StringListResponse getKnownFormats() {
    hasAccess(RESOURCE);
    return new StringListResponse(SchemaManager.getInstance().getMessageFormats());
  }

  @GET
  @Path("/link-format")
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(
      summary = "Get link-format configuration",
      description = "Retrieves the link-format configuration list.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public String getLinkFormat() {
    hasAccess(RESOURCE);
    return SchemaManager.getInstance().buildLinkFormatResponse();
  }

  private List<String> convert(List<SchemaResource> configs) throws IOException {
    List<String> data = new ArrayList<>();
    for (SchemaResource config : configs) {
      data.add(config.getDefaultVersion().pack());
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

  private static String resolveSchemaMime(SchemaConfig schemaConfig) {
    if (schemaConfig.getMimeType() != null && !schemaConfig.getMimeType().isEmpty()) {
      return schemaConfig.getMimeType();
    }
    String format = (schemaConfig.getFormat() == null) ? "" : schemaConfig.getFormat().toLowerCase(Locale.ROOT);
    return switch (format) {
      case "json" -> "application/schema+json";
      case "protobuf" -> "application/x-protobuf";
      case "avro" -> "application/avro+json";
      case "xml" -> "application/xml";
      case "cbor" -> "application/cddl";
      case "messagepack", "msgpack" -> "application/schema+json";
      case "csv" -> "application/schema+json";
      case "native", "raw" -> "application/octet-stream";
      default -> "application/octet-stream";
    };
  }

  private static String sha256Hex(byte[] bytes) {
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
    byte[] digest = messageDigest.digest(bytes);
    return java.util.HexFormat.of().formatHex(digest);
  }
}
