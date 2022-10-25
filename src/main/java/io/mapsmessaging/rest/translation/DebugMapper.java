package io.mapsmessaging.rest.translation;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class DebugMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable exception) {
    exception.printStackTrace();
    if (exception instanceof WebApplicationException) {
      return ((WebApplicationException) exception).getResponse();
    }
    return Response.serverError().entity(exception.getMessage()).build();
  }
}