package io.mapsmessaging.tools.config.lint;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionTypesTest {

  @Test
  void directClassAndParameterizedRawTypeResolveToClass() throws Exception {
    assertSame(String.class, ReflectionTypes.toClass(String.class));

    Field listField = Types.class.getDeclaredField("strings");
    Type listType = listField.getGenericType();
    assertInstanceOf(ParameterizedType.class, listType);
    assertSame(List.class, ReflectionTypes.toClass(listType));

    Field mapField = Types.class.getDeclaredField("map");
    assertSame(Map.class, ReflectionTypes.toClass(mapField.getGenericType()));
  }

  @Test
  void unsupportedTypeRepresentationsReturnNull() throws Exception {
    Field wildcard = Types.class.getDeclaredField("wildcard");
    ParameterizedType parameterized =
        assertInstanceOf(ParameterizedType.class, wildcard.getGenericType());

    Type wildcardType = parameterized.getActualTypeArguments()[0];
    assertNull(ReflectionTypes.toClass(wildcardType));
    assertNull(ReflectionTypes.toClass(null));
  }

  @Test
  void numericDetectionCoversPrimitiveAndBoxedNumericTypesOnly() {
    assertTrue(ReflectionTypes.isNumeric(int.class));
    assertTrue(ReflectionTypes.isNumeric(long.class));
    assertTrue(ReflectionTypes.isNumeric(short.class));
    assertTrue(ReflectionTypes.isNumeric(byte.class));
    assertTrue(ReflectionTypes.isNumeric(double.class));
    assertTrue(ReflectionTypes.isNumeric(float.class));

    assertTrue(ReflectionTypes.isNumeric(Integer.class));
    assertTrue(ReflectionTypes.isNumeric(Long.class));
    assertTrue(ReflectionTypes.isNumeric(Double.class));

    assertFalse(ReflectionTypes.isNumeric(boolean.class));
    assertFalse(ReflectionTypes.isNumeric(Boolean.class));
    assertFalse(ReflectionTypes.isNumeric(String.class));
    assertFalse(ReflectionTypes.isNumeric(null));
  }

  static class Types {
    List<String> strings;
    Map<String, Integer> map;
    List<? extends Number> wildcard;
  }
}
