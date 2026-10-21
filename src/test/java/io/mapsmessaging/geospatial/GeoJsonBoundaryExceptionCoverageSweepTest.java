package io.mapsmessaging.geospatial;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonBoundaryExceptionCoverageSweepTest {
  @Test
  void preservesMessageAndCauseAsIOException() {
    IllegalArgumentException cause = new IllegalArgumentException("bad geometry");
    GeoJsonBoundaryException exception =
        new GeoJsonBoundaryException("invalid boundary", cause);

    assertInstanceOf(IOException.class, exception);
    assertEquals("invalid boundary", exception.getMessage());
    assertSame(cause, exception.getCause());
  }
}
