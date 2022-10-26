package io.mapsmessaging.rest.api.impl;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.api.BaseRestApi;
import io.mapsmessaging.rest.data.SchemaData;
import io.mapsmessaging.rest.data.SchemaResponse;
import io.mapsmessaging.rest.data.StringListResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.List;
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
  @ApiOperation(value = "Get the schema configuration")
  public SchemaResponse getSchemaById(@PathParam("schemaId") String schemaId) {
    for(SchemaConfig config: SchemaManager.getInstance().getAll()){
      if(config.getUniqueId().equals(schemaId)){
        return new SchemaResponse(new SchemaData(config));
      }
    }
    return  new SchemaResponse(new ArrayList<>());
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
    return new SchemaResponse(list);
  }

  @GET
  @Path("/server/schema/formats")
  @Produces({MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Get all known formats currently supported by the schema")
  public StringListResponse getKnownFormats() {
    return new StringListResponse( SchemaManager.getInstance().getMessageFormats());
  }

  @GET
  @Path("/server/schema/link-format")
  @Produces({MediaType.TEXT_PLAIN})
  @ApiOperation(value = "Get link format string")
  public String getLinkFormat() {
    return SchemaManager.getInstance().buildLinkFormatResponse();
  }

}
