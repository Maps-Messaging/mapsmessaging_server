package io.mapsmessaging.api.transformers;

import io.mapsmessaging.selector.IdentifierResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopicNameCompilerCoverageSweepTest {
  @Test
  void resolvesSourceAndFieldsCollapsesSlashesAndRejectsWildcards() {
    IdentifierResolver resolver = mock(IdentifierResolver.class);
    when(resolver.get("systemId")).thenReturn(7);
    when(resolver.get("missing")).thenReturn(null);

    assertEquals(
        "/src/7/..../end",
        TopicNameCompiler.computeTopicName(
            "/{source}//{systemId}/{missing}/end",
            "src",
            resolver));

    when(resolver.get("systemId")).thenReturn("bad#value");
    assertThrows(
        IllegalArgumentException.class,
        () -> TopicNameCompiler.computeTopicName("/{systemId}", "src", resolver));
  }
}
