package io.mapsmessaging.tools.config.lint;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RootDtoResolverTest {

  @Test
  void directDtoSubclassResolvesToItself() {
    assertSame(ExampleDto.class, RootDtoResolver.resolveRootDto(ExampleDto.class));
  }

  @Test
  void deeperDtoSubclassResolvesToMostSpecificDtoClass() {
    assertSame(ChildDto.class, RootDtoResolver.resolveRootDto(ChildDto.class));
  }

  @Test
  void baseDtoAndUnrelatedClassesDoNotResolve() {
    assertNull(RootDtoResolver.resolveRootDto(BaseConfigDTO.class));
    assertNull(RootDtoResolver.resolveRootDto(String.class));
    assertNull(RootDtoResolver.resolveRootDto(null));
  }

  static class ExampleDto extends BaseConfigDTO {
  }

  static class ChildDto extends ExampleDto {
  }
}
