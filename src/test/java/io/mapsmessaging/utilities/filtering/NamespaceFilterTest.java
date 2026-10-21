package io.mapsmessaging.utilities.filtering;

import io.mapsmessaging.dto.rest.config.protocol.NamespaceFilterDTO;
import io.mapsmessaging.selector.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceFilterTest {

  @Test
  void emptySelectorKeepsConfigurationWithoutExecutor() throws Exception {
    NamespaceFilterDTO dto = dto("/root", "");

    NamespaceFilter filter = new NamespaceFilter(dto);

    assertSame(dto, filter.getConfig());
    assertNull(filter.getExecutor());
  }

  @Test
  void validSelectorCompilesExecutor() throws Exception {
    NamespaceFilter filter = new NamespaceFilter(dto("/root", "1 = 1"));

    assertNotNull(filter.getExecutor());
  }

  @Test
  void invalidSelectorIsWrappedAsIOException() {
    IOException failure = assertThrows(
        IOException.class,
        () -> new NamespaceFilter(dto("/root", "THIS IS NOT A SELECTOR !!!"))
    );

    assertInstanceOf(ParseException.class, failure.getCause());
  }

  private static NamespaceFilterDTO dto(String namespace, String selector) {
    NamespaceFilterDTO dto = new NamespaceFilterDTO();
    dto.setNamespace(namespace);
    dto.setSelector(selector);
    dto.setDepth(2);
    dto.setForcePriority(true);
    return dto;
  }
}
