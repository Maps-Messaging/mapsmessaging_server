/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.tools.config.lint;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DtoWalkerTest {

  @Test
  void walkerTraversesNestedDtoListAndMapTypesAndReportsRawCollections() {
    LintEngine engine = quietEngine();

    List<LintIssue> issues = new DtoWalker(engine).lint("test", RootDto.class);

    assertTrue(issues.stream().anyMatch(issue ->
        issue.getRuleId().equals("GENERIC_TYPE_ERASED")
            && issue.getPath().endsWith("rawList")));

    verify(engine, atLeastOnce()).lintClass(
        eq("test"),
        eq(RootDto.class.getName()),
        eq(RootDto.class),
        eq("RootDto")
    );
    verify(engine, atLeastOnce()).lintClass(
        eq("test"),
        eq(RootDto.class.getName()),
        eq(ChildDto.class),
        anyString()
    );
  }

  @Test
  void duplicatePolymorphicDiscriminatorsAreReported() {
    LintEngine engine = quietEngine();

    List<LintIssue> issues = new DtoWalker(engine).lint("poly", PolymorphicRoot.class);

    assertTrue(issues.stream().anyMatch(issue ->
        issue.getRuleId().equals("POLYMORPHIC_DISCRIMINATOR_COLLISION")));
  }

  @Test
  void jsonTypeInfoWithoutSubtypesProducesWarning() {
    LintEngine engine = quietEngine();

    List<LintIssue> issues = new DtoWalker(engine).lint("poly", TypeInfoOnlyRoot.class);

    assertTrue(issues.stream().anyMatch(issue ->
        issue.getRuleId().equals("POLYMORPHIC_SUBTYPES_MISSING")
            && issue.getSeverity() == LintSeverity.WARN));
  }

  private static LintEngine quietEngine() {
    LintEngine engine = mock(LintEngine.class);
    when(engine.lintClass(anyString(), anyString(), any(), anyString())).thenReturn(List.of());
    when(engine.lintField(anyString(), anyString(), any(), any(), anyString())).thenReturn(List.of());
    return engine;
  }

  static class RootDto extends BaseConfigDTO {
    ChildDto child;
    List<ChildDto> children;
    Map<String, ChildDto> childMap;
    @SuppressWarnings("rawtypes")
    List rawList;
  }

  static class ChildDto extends BaseConfigDTO {
    String value;
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
  @JsonSubTypes({
      @JsonSubTypes.Type(value = PolyA.class, name = "same"),
      @JsonSubTypes.Type(value = PolyB.class, name = "same")
  })
  static class PolymorphicRoot extends BaseConfigDTO {
  }

  static class PolyA extends PolymorphicRoot {
  }

  static class PolyB extends PolymorphicRoot {
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
  static class TypeInfoOnlyRoot extends BaseConfigDTO {
  }
}
