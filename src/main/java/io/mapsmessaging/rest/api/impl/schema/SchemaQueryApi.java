/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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
 *
 */

package io.mapsmessaging.rest.api.impl.schema;

import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.schema.SchemaPostData;
import io.mapsmessaging.rest.responses.*;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Schema Management")
@Path(URI_PATH)
public class SchemaQueryApi extends BaseRestApi {

  @DELETE
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Delete specific schema", description = "Delete the schema configuration by unique id")
  public BaseResponse deleteSchemaById(@PathParam("schemaId") String schemaId) {
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config != null) {
      SchemaManager.getInstance().removeSchema(schemaId);
      return new BaseResponse(request);
    }
    return new BaseResponse(request);
  }

  @DELETE
  @Path("/server/schema")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Delete all schemas", description = "Deletes all the schema configurations")
  public BaseResponse deleteAllSchemas() {
    SchemaManager.getInstance().removeAllSchemas();
    return new BaseResponse(request);
  }

  //@ApiOperation(value = "Add a new schema configuration to the repository")
  @Path("/server/schema")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Operation(summary = "Add new schema", description = "Adds a new schema to the registry")
  public BaseResponse addSchema(SchemaPostData jsonString) throws IOException {
    SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(jsonString.getSchema());
    SchemaManager.getInstance().addSchema(jsonString.getContext(), config);
    return new BaseResponse(request);
  }

  @GET
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get schema", description = "Returns a specific schema")
  public SchemaResponse getSchemaById(@PathParam("schemaId") String schemaId) throws IOException {
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if (config != null) {
      return new SchemaResponse(request, config.pack());
    }
    return new SchemaResponse(request, new ArrayList<>());
  }

  @GET
  @Path("/server/schema/context/{context}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get schema by context", description = "Returns all schemas that match the context")
  public SchemaResponse getSchemaByContext(@PathParam("context") String context) throws IOException {
    List<SchemaConfig> config = SchemaManager.getInstance().getSchemaByContext(context);
    if (config != null) {
      return new SchemaResponse(request, convert(config));
    }
    return new SchemaResponse(request, new ArrayList<>());
  }

  @GET
  @Path("/server/schema/type/{type}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get schema by type", description = "Returns all schemas that match the type")
  public SchemaResponse getSchemaByType(@PathParam("type") String type) throws IOException {
    List<SchemaConfig> config = SchemaManager.getInstance().getSchemas(type);
    if (config != null) {
      return new SchemaResponse(request, convert(config));
    }
    return new SchemaResponse(request, new ArrayList<>());
  }

  @GET
  @Path("/server/schema")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get all schemas", description = "Returns all schemas")
  public SchemaConfigResponse getAllSchemas() {
    return new SchemaConfigResponse(request, SchemaManager.getInstance().getAll());
  }

  @GET
  @Path("/server/schema/map")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get schemas and the configuration", description = "Returns all schemas and mapping information")
  public SchemaMapResponse getSchemaMapping()  {
    Map<String, List<SchemaConfig>> map = SchemaManager.getInstance().getMappedSchemas();
    Map<String, List<String>> responseMap = new LinkedHashMap<>();
    for (Entry<String, List<SchemaConfig>> entry : map.entrySet()) {
      responseMap.put(entry.getKey(), (convertToId(entry.getValue())));
    }
    return new SchemaMapResponse(request, responseMap);
  }

  @GET
  @Path("/server/schema/formats")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(summary = "Get all known formats supported", description = "Returns list of supported formats")
  public StringListResponse getKnownFormats() {
    return new StringListResponse(request, SchemaManager.getInstance().getMessageFormats());
  }

  @GET
  @Path("/server/schema/link-format")
  @Produces({MediaType.TEXT_PLAIN})
  @Operation(summary = "Get the link-format config", description = "Returns link-format list")
  public String getLinkFormat() {
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
