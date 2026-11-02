package io.mapsmessaging.rest.api.pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class PaginationService {
  public static final int DEFAULT_PAGE_SIZE = 100;
  public static final int MAX_PAGE_SIZE = 1000;
  private final CursorCodec codec;

  public PaginationService() {
    this(new CursorCodec());
  }

  PaginationService(CursorCodec codec) {
    this.codec = codec;
  }

  public <T> PageResponse<T> page(List<T> visibleEntries, Function<T, String> keyExtractor,
                                  PageRequest request, String scope) {
    Objects.requireNonNull(visibleEntries);
    Objects.requireNonNull(keyExtractor);
    Objects.requireNonNull(request);
    Objects.requireNonNull(scope);
    int pageSize = request.pageSize();
    if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
    }
    String after = codec.decode(request.cursor(), scope);
    List<T> sorted = new ArrayList<>(visibleEntries);
    sorted.sort(Comparator.comparing(keyExtractor));
    List<T> page = new ArrayList<>(Math.min(pageSize, sorted.size()));
    String previous = null;
    boolean hasMore = false;
    for (T entry : sorted) {
      String key = Objects.requireNonNull(keyExtractor.apply(entry), "Pagination key must not be null");
      if (previous != null && previous.equals(key)) {
        throw new IllegalArgumentException("Pagination keys must be unique");
      }
      previous = key;
      if (after != null && key.compareTo(after) <= 0) {
        continue;
      }
      if (page.size() == pageSize) {
        hasMore = true;
        break;
      }
      page.add(entry);
    }
    String nextCursor = hasMore ? codec.encode(keyExtractor.apply(page.get(page.size() - 1)), scope) : null;
    return new PageResponse<>(List.copyOf(page), nextCursor, hasMore);
  }
}
