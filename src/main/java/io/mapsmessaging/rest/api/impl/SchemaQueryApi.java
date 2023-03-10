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
import io.mapsmessaging.rest.data.SchemaData;
import io.mapsmessaging.rest.responses.SchemaMapResponse;
import io.mapsmessaging.rest.responses.SchemaResponse;
import io.mapsmessaging.rest.responses.StringListResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = URI_PATH+ "/server/schema", tags="Schema Management")
@Path(URI_PATH)
public class SchemaQueryApi extends BaseRestApi {

  @GET
  @Path("/server/schema/{schemaId}")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get the schema configuration by unique id")
  public SchemaResponse getSchemaById(@PathParam("schemaId") String schemaId) {
    SchemaConfig config = SchemaManager.getInstance().getSchema(schemaId);
    if(config != null) {
      return new SchemaResponse(request, new SchemaData(config));
    }
    return  new SchemaResponse(request, new ArrayList<>());
  }

  @GET
  @Path("/server/schema/context/{context}")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get the schema configuration by context name")
  public SchemaResponse getSchemaByContext(@PathParam("context") String context) {
    List<SchemaConfig> config = SchemaManager.getInstance().getSchemaByContext(context);
    if(config != null) {
      List<SchemaData> list = new ArrayList<>();
      for(SchemaConfig con:config){
        list.add(new SchemaData(con));
      }
      return new SchemaResponse(request, list);
    }
    return  new SchemaResponse(request, new ArrayList<>());
  }

  @GET
  @Path("/server/schema/type/{type}")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get the schema configuration by schema type")
  public SchemaResponse getSchemaByType(@PathParam("type") String type) {
    List<SchemaConfig> config = SchemaManager.getInstance().getSchemas(type);
    if(config != null) {
      List<SchemaData> list = new ArrayList<>();
      for(SchemaConfig con:config){
        list.add(new SchemaData(con));
      }
      return new SchemaResponse(request, list);
    }
    return  new SchemaResponse(request, new ArrayList<>());
  }

  @GET
  @Path("/server/schema")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get all the schema configuration")
  public SchemaResponse getAllSchemas() {
    List<SchemaData> list = new ArrayList<>();
    for(SchemaConfig config:SchemaManager.getInstance().getAll()){
      list.add(new SchemaData(config));
    }
    return new SchemaResponse(request, list);
  }

  @GET
  @Path("/server/schema/map")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get all the schema configuration")
  public SchemaMapResponse getSchemaMapping() {
    Map<String, List<SchemaConfig>> map = SchemaManager.getInstance().getMappedSchemas();

    return new SchemaMapResponse(request, new LinkedHashMap<>());
  }



  @GET
  @Path("/server/schema/formats")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get all known formats currently supported by the schema")
  public StringListResponse getKnownFormats() {
    return new StringListResponse(request,  SchemaManager.getInstance().getMessageFormats());
  }

  @GET
  @Path("/server/schema/link-format")
  @Produces({MediaType.TEXT_PLAIN})
  @ApiOperation(value = "Get link format string")
  public String getLinkFormat() {
    return SchemaManager.getInstance().buildLinkFormatResponse();
  }

}
