package io.mapsmessaging.rest.api.impl.destination;

import io.mapsmessaging.dto.rest.destination.DestinationDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DestinationManagementApiTest {

  @Test
  void sortDestinationListSupportsEveryDocumentedSortKey() throws Exception {
    DestinationManagementApi api = new DestinationManagementApi();
    Method sort = DestinationManagementApi.class.getDeclaredMethod("sortDestinationList", List.class, String.class);
    sort.setAccessible(true);

    assertFirst(sort, api, "Name", first("alpha", 1), first("zulu", 2), "zulu");
    assertFirst(sort, api, "Published", first("alpha", 1), first("zulu", 2), "zulu");
    assertFirst(sort, api, "Delivered", first("alpha", 1), first("zulu", 2), "zulu");
    assertFirst(sort, api, "Stored", first("alpha", 1), first("zulu", 2), "zulu");
    assertFirst(sort, api, "Pending", first("alpha", 1), first("zulu", 2), "zulu");
    assertFirst(sort, api, "Delayed", first("alpha", 1), first("zulu", 2), "zulu");
    assertFirst(sort, api, "Expired", first("alpha", 1), first("zulu", 2), "zulu");
  }

  @Test
  void invalidSortKeyIsRejected() throws Exception {
    DestinationManagementApi api = new DestinationManagementApi();
    Method sort = DestinationManagementApi.class.getDeclaredMethod("sortDestinationList", List.class, String.class);
    sort.setAccessible(true);

    InvocationTargetException thrown = assertThrows(
        InvocationTargetException.class,
        () -> sort.invoke(api, new ArrayList<>(List.of(first("a", 1), first("b", 2))), "Bogus"));

    assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
    assertTrue(thrown.getCause().getMessage().contains("Invalid sortBy"));
  }

  private static void assertFirst(
      Method sort,
      DestinationManagementApi api,
      String key,
      DestinationDTO a,
      DestinationDTO b,
      String expectedName) throws Exception {
    List<DestinationDTO> values = new ArrayList<>(List.of(a, b));
    sort.invoke(api, values, key);
    assertEquals(expectedName, values.getFirst().getName());
  }

  private static DestinationDTO first(String name, long value) {
    DestinationDTO dto = new DestinationDTO();
    dto.setName(name);
    dto.setPublishedMessages(value);
    dto.setDeliveredMessages(value);
    dto.setStoredMessages(value);
    dto.setPendingMessages(value);
    dto.setDelayedMessages(value);
    dto.setExpiredMessages(value);
    return dto;
  }
}