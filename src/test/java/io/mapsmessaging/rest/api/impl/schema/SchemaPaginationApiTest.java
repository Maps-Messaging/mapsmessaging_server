package io.mapsmessaging.rest.api.impl.schema;

import io.mapsmessaging.rest.ApiTestBase;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static org.junit.jupiter.api.Assertions.*;

class SchemaPaginationApiTest extends ApiTestBase {
  private static final String PATH = URI_PATH + "/server/schemas/page";

  @Test
  void traversesSchemaPages() {
    Response first = givenAuthenticated().queryParam("pageSize", 1).get(PATH);
    assertEquals(200, first.statusCode());
    assertEquals(1, first.jsonPath().getList("entries").size());
    assertTrue(first.jsonPath().getBoolean("hasMore"));
    String cursor = first.jsonPath().getString("nextCursor");
    assertNotNull(cursor);

    Response second = givenAuthenticated().queryParam("pageSize", 1)
        .queryParam("cursor", cursor).get(PATH);
    assertEquals(200, second.statusCode());
    assertEquals(1, second.jsonPath().getList("entries").size());
    assertNotEquals(first.jsonPath().getString("entries[0].uniqueId"),
        second.jsonPath().getString("entries[0].uniqueId"));
  }

  @Test
  void rejectsInvalidCursorAndPageSize() {
    assertEquals(400, givenAuthenticated().queryParam("cursor", "invalid.token").get(PATH).statusCode());
    assertEquals(400, givenAuthenticated().queryParam("pageSize", 0).get(PATH).statusCode());
    assertEquals(400, givenAuthenticated().queryParam("pageSize", 1001).get(PATH).statusCode());
    assertEquals(400, givenAuthenticated().queryParam("filter", "this is not a selector expression {").get(PATH).statusCode());
  }
}
