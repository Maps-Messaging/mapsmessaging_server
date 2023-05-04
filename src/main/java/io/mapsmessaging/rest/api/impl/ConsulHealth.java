package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.rest.api.BaseRestApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = "/")
@Path("/")
public class ConsulHealth extends BaseRestApi {


  @GET
  @Produces({MediaType.TEXT_PLAIN})
  @ApiOperation(value = "Simple request to test if the server is running")
  public String getPing() {
    return "Ok";
  }

}
