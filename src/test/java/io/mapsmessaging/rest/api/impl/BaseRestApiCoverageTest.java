/*
 *
 * Copyright [ 2020 - 2024 ] Matthew Buckton
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */

package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseRestApiCoverageTest {

  @ParameterizedTest
  @MethodSource("uuidCases")
  void parseUuidOrNullHandlesValidAndInvalidValues(String value, boolean expectedPresent) {
    ExposedBaseRestApi api = new ExposedBaseRestApi();

    UUID result = api.parseUuid(value);

    assertEquals(expectedPresent, result != null);
  }

  @Test
  void withUuidInvokesHandlerForValidUuid() {
    ExposedBaseRestApi api = new ExposedBaseRestApi();
    UUID uuid = UUID.randomUUID();
    AtomicReference<UUID> seen = new AtomicReference<>();

    Response response = api.withUuid(uuid.toString(), parsed -> {
      seen.set(parsed);
      return Response.ok("ok").build();
    });

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals(uuid, seen.get());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "not-a-uuid"})
  void withUuidRejectsInvalidUuid(String value) {
    ExposedBaseRestApi api = new ExposedBaseRestApi();

    Response response = api.withUuid(value, ignored -> Response.ok().build());

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertInstanceOf(StatusResponse.class, response.getEntity());
  }

  @Test
  void withUuidRejectsNullUuid() {
    ExposedBaseRestApi api = new ExposedBaseRestApi();

    Response response = api.withUuid(null, ignored -> Response.ok().build());

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void requireNonNullRejectsNullAndInvokesHandlerOtherwise() {
    ExposedBaseRestApi api = new ExposedBaseRestApi();

    Response rejected = api.require("value", null, () -> Response.ok().build());
    Response accepted = api.require("value", "present", () -> Response.ok("yes").build());

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rejected.getStatus());
    assertEquals(Response.Status.OK.getStatusCode(), accepted.getStatus());
    assertEquals("yes", accepted.getEntity());
  }

  @ParameterizedTest
  @CsvSource({
      "GET,0",
      "HEAD,0",
      "POST,1",
      "PUT,2",
      "DELETE,3",
      "PATCH,0",
      "OPTIONS,0"
  })
  void computeAccessMapsHttpMethods(String method, long expected) throws Exception {
    ExposedBaseRestApi api = new ExposedBaseRestApi();

    assertEquals(expected, api.computeAccess(method));
  }

  @Test
  void extractClientIpPrefersFirstForwardedAddress() {
    ExposedBaseRestApi api = new ExposedBaseRestApi();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn(" 10.0.0.1, 10.0.0.2 ");
    when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.3");
    when(request.getRemoteAddr()).thenReturn("10.0.0.4");
    api.setRequest(request);

    assertEquals("10.0.0.1", api.extractClientIp());
  }

  @Test
  void extractClientIpUsesRealIpWhenForwardedMissingOrBlank() {
    ExposedBaseRestApi api = new ExposedBaseRestApi();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn(" ");
    when(request.getHeader("X-Real-IP")).thenReturn(" 10.0.0.3 ");
    when(request.getRemoteAddr()).thenReturn("10.0.0.4");
    api.setRequest(request);

    assertEquals("10.0.0.3", api.extractClientIp());
  }

  @Test
  void extractClientIpFallsBackToRemoteAddress() {
    ExposedBaseRestApi api = new ExposedBaseRestApi();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("10.0.0.4");
    api.setRequest(request);

    assertEquals("10.0.0.4", api.extractClientIp());
  }

  @Test
  void responseBuildersReturnExpectedStatusesAndEntities() {
    ExposedBaseRestApi api = new ExposedBaseRestApi();

    assertEquals(200, api.okResponse("ok").getStatus());
    assertEquals(201, api.createdResponse(new StatusResponse("made")).getStatus());
    assertEquals(204, api.noContentResponse().getStatus());
    assertEquals(400, api.badRequestResponse().getStatus());
    assertEquals(400, api.badRequestResponse("bad").getStatus());
    assertEquals(404, api.notFoundResponse().getStatus());
    assertEquals(404, api.notFoundResponse("missing").getStatus());
    assertEquals(409, api.conflictResponse("conflict").getStatus());
    assertEquals(500, api.internalServerErrorResponse("error").getStatus());
  }

  private static Stream<Arguments> uuidCases() {
    return Stream.of(
        Arguments.of(null, false),
        Arguments.of("", false),
        Arguments.of(" ", false),
        Arguments.of("not-a-uuid", false),
        Arguments.of("00000000-0000-0000-0000-000000000000", true),
        Arguments.of(UUID.randomUUID().toString(), true)
    );
  }

  private static final class ExposedBaseRestApi extends BaseRestApi {
    UUID parseUuid(String value) {
      return parseUuidOrNull(value);
    }

    Response withUuid(String value, java.util.function.Function<UUID, Response> handler) {
      return withUuidOrBadRequest(value, handler);
    }

    <T> Response require(String message, T value, java.util.function.Supplier<Response> handler) {
      return requireNonNull(value, message, handler);
    }

    long computeAccess(String method) throws Exception {
      Method m = BaseRestApi.class.getDeclaredMethod("computeAccess", String.class);
      m.setAccessible(true);
      return (long) m.invoke(this, method);
    }

    void setRequest(HttpServletRequest request) {
      this.request = request;
    }

    Response okResponse(Object entity) { return ok(entity); }
    Response createdResponse(StatusResponse entity) { return created(entity); }
    Response noContentResponse() { return noContent(); }
    Response badRequestResponse() { return badRequest(); }
    Response badRequestResponse(String message) { return badRequest(message); }
    Response notFoundResponse() { return notFound(); }
    Response notFoundResponse(String message) { return notFound(message); }
    Response conflictResponse(String message) { return conflict(message); }
    Response internalServerErrorResponse(String message) { return internalServerError(message); }
  }
}
