package io.mapsmessaging.api.transformers;

import io.mapsmessaging.selector.IdentifierResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpandedIdentityLookupCoverageSweepTest {
  @Test
  void extraValuesOverrideBaseAndMissingExtrasFallBackToBaseResolver() {
    IdentifierResolver base = mock(IdentifierResolver.class);
    when(base.get("base")).thenReturn("base-value");
    when(base.get("override")).thenReturn("old");

    ExpandedIdentityLookup lookup =
        new ExpandedIdentityLookup(Map.of("override", "new"), base);

    assertEquals("new", lookup.get("override"));
    assertEquals("base-value", lookup.get("base"));
    verify(base, never()).get("override");
  }
}
