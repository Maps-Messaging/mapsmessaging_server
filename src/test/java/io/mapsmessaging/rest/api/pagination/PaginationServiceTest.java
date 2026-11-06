package io.mapsmessaging.rest.api.pagination;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PaginationServiceTest {
  private final PaginationService service = new PaginationService();

  @Test
  void traversesAllPagesInSortedOrder() {
    List<String> values = List.of("c", "a", "b", "d", "e");
    PageResponse<String> first = service.page(values, s -> s, new PageRequest(2, null), "schemas");
    assertEquals(List.of("a", "b"), first.entries());
    assertTrue(first.hasMore());
    PageResponse<String> second = service.page(values, s -> s, new PageRequest(2, first.nextCursor()), "schemas");
    assertEquals(List.of("c", "d"), second.entries());
    PageResponse<String> last = service.page(values, s -> s, new PageRequest(2, second.nextCursor()), "schemas");
    assertEquals(List.of("e"), last.entries());
    assertFalse(last.hasMore());
    assertNull(last.nextCursor());
  }

  @Test
  void emptyCollectionHasNoContinuation() {
    PageResponse<String> result = service.page(List.of(), s -> s, new PageRequest(10, null), "schemas");
    assertTrue(result.entries().isEmpty());
    assertFalse(result.hasMore());
  }

  @Test
  void rejectsInvalidSizesDuplicateKeysAndTamperedCursors() {
    assertThrows(IllegalArgumentException.class, () -> service.page(List.of("a"), s -> s, new PageRequest(0, null), "schemas"));
    assertThrows(IllegalArgumentException.class, () -> service.page(List.of("a"), s -> s, new PageRequest(1001, null), "schemas"));
    assertThrows(IllegalArgumentException.class, () -> service.page(List.of("a", "a"), s -> s, new PageRequest(1, null), "schemas"));
    assertThrows(IllegalArgumentException.class, () -> service.page(List.of("a"), s -> s, new PageRequest(1, "bad.token"), "schemas"));
  }

  @Test
  void cursorIsBoundToScopeAndChangesAreKeysetBased() {
    PageResponse<String> first = service.page(List.of("a", "c", "e"), s -> s, new PageRequest(1, null), "schemas:user1");
    assertThrows(IllegalArgumentException.class, () ->
        service.page(List.of("a", "c"), s -> s, new PageRequest(1, first.nextCursor()), "schemas:user2"));
    PageResponse<String> next = service.page(List.of("a", "b", "c", "e"), s -> s,
        new PageRequest(1, first.nextCursor()), "schemas:user1");
    assertEquals(List.of("b"), next.entries());
  }
}
