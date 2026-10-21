package io.mapsmessaging.state.rest.twins;

import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class TwinManagementApiTest {

  @Test
  void unauthorizedAndForbiddenExceptionsAreMappedToJsonResponses() throws Exception {
    TwinManagementApi api = new TwinManagementApi();
    Method mapper = TwinManagementApi.class.getDeclaredMethod("mapAuthOrRethrow", WebApplicationException.class);
    mapper.setAccessible(true);

    Response unauthorized = (Response) mapper.invoke(api, new WebApplicationException(401));
    Response forbidden = (Response) mapper.invoke(api, new WebApplicationException(403));

    assertEquals(401, unauthorized.getStatus());
    assertEquals("Unauthorized", ((StatusResponse) unauthorized.getEntity()).getStatus());
    assertEquals(403, forbidden.getStatus());
    assertEquals("Access denied", ((StatusResponse) forbidden.getEntity()).getStatus());
  }

  @Test
  void nonAuthenticationWebExceptionIsRethrown() throws Exception {
    TwinManagementApi api = new TwinManagementApi();
    Method mapper = TwinManagementApi.class.getDeclaredMethod("mapAuthOrRethrow", WebApplicationException.class);
    mapper.setAccessible(true);
    WebApplicationException original = new WebApplicationException(418);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, () -> mapper.invoke(api, original));

    assertSame(original, thrown.getCause());
  }
}