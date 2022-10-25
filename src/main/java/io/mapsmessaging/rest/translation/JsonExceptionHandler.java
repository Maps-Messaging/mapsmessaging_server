package io.mapsmessaging.rest.translation;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonExceptionHandler implements ExceptionMapper<JsonMappingException> {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public Response toResponse(JsonMappingException exception) {
    ObjectNode json = mapper.createObjectNode();
    json.put("error", "json mapping error");
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(json.toPrettyString())
        .build();
  }

}