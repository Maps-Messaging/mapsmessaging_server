package io.mapsmessaging.api.message;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypedDataTest {

  @Test
  void scalarTypesAreClassifiedWithoutCoercion() {
    assertTyped((byte) 1, TypedData.TYPE.BYTE);
    assertTyped((short) 2, TypedData.TYPE.SHORT);
    assertTyped(3, TypedData.TYPE.INT);
    assertTyped(4L, TypedData.TYPE.LONG);
    assertTyped(5.0f, TypedData.TYPE.FLOAT);
    assertTyped(6.0d, TypedData.TYPE.DOUBLE);
    assertTyped(true, TypedData.TYPE.BOOLEAN);
    assertTyped("text", TypedData.TYPE.STRING);
    assertTyped('x', TypedData.TYPE.CHAR);
  }

  @Test
  void arrayAndMapTypesAreRecognised() {
    assertTyped(new byte[]{1}, TypedData.TYPE.BYTE_ARRAY);
    assertTyped(new short[]{1}, TypedData.TYPE.SHORT_ARRAY);
    assertTyped(new int[]{1}, TypedData.TYPE.INT_ARRAY);
    assertTyped(new long[]{1}, TypedData.TYPE.LONG_ARRAY);
    assertTyped(new float[]{1}, TypedData.TYPE.FLOAT_ARRAY);
    assertTyped(new double[]{1}, TypedData.TYPE.DOUBLE_ARRAY);
    assertTyped(new String[]{"a"}, TypedData.TYPE.STRING_ARRAY);
    assertTyped(new char[]{'a'}, TypedData.TYPE.CHAR_ARRAY);
    assertTyped(Map.of("a", 1), TypedData.TYPE.TYPED_MAP);
  }

  @Test
  void unknownObjectsFallBackToStableStringRepresentation() {
    Object value = new Object() {
      @Override
      public String toString() {
        return "custom-value";
      }
    };

    TypedData typed = new TypedData(value);

    assertEquals(TypedData.TYPE.STRING, typed.getType());
    assertEquals("custom-value", typed.getData());
  }

  @Test
  void wireTypeLookupReturnsNullForUnknownCode() {
    assertEquals(TypedData.TYPE.INT, TypedData.TYPE.valueOf(2));
    assertNull(TypedData.TYPE.valueOf(99));
  }

  private static void assertTyped(Object value, TypedData.TYPE expected) {
    TypedData typed = new TypedData(value);
    assertEquals(expected, typed.getType());
    assertSame(value, typed.getData());
  }
}
