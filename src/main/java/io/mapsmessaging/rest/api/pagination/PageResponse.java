package io.mapsmessaging.rest.api.pagination;

import java.util.List;

public record PageResponse<T>(List<T> entries, String nextCursor, boolean hasMore) {
}
