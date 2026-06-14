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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LintUtilityTest {

  @Test
  void looksLikeEnum_detectsTokensCaseInsensitivelyAndExcludesOpenVocabulary() {
    assertTrue(StringEnumHeuristics.looksLikeEnum("connectionMode"));
    assertTrue(StringEnumHeuristics.looksLikeEnum("PROTOCOL"));
    assertTrue(StringEnumHeuristics.looksLikeEnum("strategyName"));
    assertFalse(StringEnumHeuristics.looksLikeEnum("contentType"));
    assertFalse(StringEnumHeuristics.looksLikeEnum("description"));
    assertFalse(StringEnumHeuristics.looksLikeEnum(null));
    assertFalse(StringEnumHeuristics.looksLikeEnum(" "));
  }

  @Test
  void reflectionTypes_resolvesClassesAndParameterizedRawTypes() throws Exception {
    Type listType = TypeHolder.class.getDeclaredField("names").getGenericType();
    Type unknownType = new Type() {
      @Override
      public String getTypeName() {
        return "unknown";
      }
    };

    assertEquals(String.class, ReflectionTypes.toClass(String.class));
    assertEquals(List.class, ReflectionTypes.toClass(listType));
    assertNull(ReflectionTypes.toClass(unknownType));
  }

  @Test
  void reflectionTypes_numericDetection_excludesBooleanCharacterAndBigDecimal() {
    assertTrue(ReflectionTypes.isNumeric(int.class));
    assertTrue(ReflectionTypes.isNumeric(Double.class));
    assertFalse(ReflectionTypes.isNumeric(boolean.class));
    assertFalse(ReflectionTypes.isNumeric(Character.class));
    assertFalse(ReflectionTypes.isNumeric(java.math.BigDecimal.class));
    assertFalse(ReflectionTypes.isNumeric(null));
  }

  @Test
  void reflectionFields_returnsInheritedInstanceFieldsAndFiltersStaticAndTransient() {
    List<Field> fields = ReflectionFields.getAllInstanceFields(ChildFields.class);
    Set<String> fieldNames = fields.stream().map(Field::getName).collect(Collectors.toSet());

    assertEquals(Set.of("childValue", "parentValue"), fieldNames);
    assertTrue(fields.stream().allMatch(Field::trySetAccessible));
  }

  private static final class TypeHolder {
    private List<String> names;
  }

  private static class ParentFields {
    private static String staticValue;
    private transient String transientValue;
    private String parentValue;
  }

  private static final class ChildFields extends ParentFields {
    private String childValue;
  }
}
