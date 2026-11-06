package io.mapsmessaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildInfoCoverageSweepTest {
  @Test
  void exposesBuildConstantsThroughAccessors() {
    assertEquals(BuildInfo.BUILD_DATE, BuildInfo.getBuildDate());
    assertEquals(BuildInfo.BUILD_VERSION, BuildInfo.getBuildVersion());
    assertFalse(BuildInfo.getBuildDate().isBlank());
    assertFalse(BuildInfo.getBuildVersion().isBlank());
  }
}
