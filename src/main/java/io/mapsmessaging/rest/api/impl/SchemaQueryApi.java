/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.api.impl;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.api.BaseRestApi;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.SchemaMapResponse;
import io.mapsmessaging.rest.responses.SchemaResponse;
import io.mapsmessaging.rest.responses.StringListResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = URI_PATH + "/server/schema", tags = "Schema Management")
@Path(URI_PATH)
public class SchemaQueryApi extends BaseRestApi {

  @DELETE
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Delete the schema configuration by unique id")
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
  @ApiOperation(value = "Delete all schema configuration in the repository")
  public BaseResponse deleteAllSchemas() {
    SchemaManager.getInstance().removeAllSchemas();
    return new BaseResponse(request);
  }

  @GET
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get the schema configuration by unique id")
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
  @ApiOperation(value = "Get the schema configuration by context name")
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
  @ApiOperation(value = "Get the schema configuration by schema type")
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
  @ApiOperation(value = "Get all the schema configuration")
  public SchemaResponse getAllSchemas() throws IOException {
    return new SchemaResponse(request, convert(SchemaManager.getInstance().getAll()));
  }

  @GET
  @Path("/server/schema/map")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get all the schema configuration")
  public SchemaMapResponse getSchemaMapping() throws IOException {
    Map<String, List<SchemaConfig>> map = SchemaManager.getInstance().getMappedSchemas();
    Map<String, List<String>> responseMap = new LinkedHashMap<>();
    for (Entry<String, List<SchemaConfig>> entry : map.entrySet()) {
      responseMap.put(entry.getKey(), (convert(entry.getValue())));
    }
    return new SchemaMapResponse(request, responseMap);
  }

  @GET
  @Path("/server/schema/formats")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get all known formats currently supported by the schema")
  public StringListResponse getKnownFormats() {
    return new StringListResponse(request, SchemaManager.getInstance().getMessageFormats());
  }

  @GET
  @Path("/server/schema/link-format")
  @Produces({MediaType.TEXT_PLAIN})
  @ApiOperation(value = "Get link format string")
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

}
