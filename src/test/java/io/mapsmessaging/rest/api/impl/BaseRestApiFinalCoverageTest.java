package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class BaseRestApiFinalCoverageTest {

  @Test
  void successResponseHelpersUseExpectedStatusCodesAndEntities() {
    TestApi api = new TestApi();
    Object entity = new Object();

    Response ok = api.okResponse(entity);
    assertEquals(200, ok.getStatus());
    assertSame(entity, ok.getEntity());

    StatusResponse createdEntity = new StatusResponse("created");
    Response created = api.createdResponse(createdEntity);
    assertEquals(201, created.getStatus());
    assertSame(createdEntity, created.getEntity());

    assertEquals(204, api.noContentResponse().getStatus());
  }

  @Test
  void errorResponseHelpersPreserveStatusAndMessage() {
    TestApi api = new TestApi();

    assertEquals(400, api.badRequestResponse().getStatus());
    assertEquals(
        "bad",
        ((StatusResponse) api.badRequestResponse("bad").getEntity()).getStatus());
    assertEquals(404, api.notFoundResponse().getStatus());
    assertEquals(
        "missing",
        ((StatusResponse) api.notFoundResponse("missing").getEntity()).getStatus());
    assertEquals(
        "conflict",
        ((StatusResponse) api.conflictResponse("conflict").getEntity()).getStatus());
    assertEquals(
        "broken",
        ((StatusResponse) api.errorResponse("broken").getEntity()).getStatus());
  }

  @Test
  void uuidHandlerRejectsInvalidInputAndInvokesHandlerForValidUuid() {
    TestApi api = new TestApi();
    UUID uuid = UUID.randomUUID();
    AtomicBoolean called = new AtomicBoolean();

    Response invalid = api.withUuid("not-a-uuid", value -> {
      called.set(true);
      return Response.ok().build();
    });
    assertEquals(400, invalid.getStatus());
    assertFalse(called.get());

    Response valid = api.withUuid(uuid.toString(), value -> {
      called.set(true);
      assertEquals(uuid, value);
      return Response.accepted().build();
    });
    assertEquals(202, valid.getStatus());
    assertTrue(called.get());
  }

  @Test
  void requireNonNullShortCircuitsNullAndRunsHandlerForPresentValue() {
    TestApi api = new TestApi();
    AtomicBoolean called = new AtomicBoolean();

    Response missing = api.require("value", null, () -> {
      called.set(true);
      return Response.ok().build();
    });
    assertEquals(400, missing.getStatus());
    assertEquals(
        "value",
        ((StatusResponse) missing.getEntity()).getStatus());
    assertFalse(called.get());

    Response present = api.require("value", "x", () -> {
      called.set(true);
      return Response.status(202).build();
    });
    assertEquals(202, present.getStatus());
    assertTrue(called.get());
  }

  private static final class TestApi extends BaseRestApi {
    Response okResponse(Object value) { return ok(value); }
    Response createdResponse(StatusResponse value) { return created(value); }
    Response noContentResponse() { return noContent(); }
    Response badRequestResponse() { return badRequest(); }
    Response badRequestResponse(String value) { return badRequest(value); }
    Response notFoundResponse() { return notFound(); }
    Response notFoundResponse(String value) { return notFound(value); }
    Response conflictResponse(String value) { return conflict(value); }
    Response errorResponse(String value) { return internalServerError(value); }
    Response withUuid(
        String value,
        java.util.function.Function<UUID, Response> handler) {
      return withUuidOrBadRequest(value, handler);
    }
    <T> Response require(String message, T value, java.util.function.Supplier<Response> handler) {
      return requireNonNull(value, message, handler);
    }
  }
}