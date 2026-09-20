package io.mapsmessaging.network.protocol.impl.coap.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClazzTest {

  @Test
  void numericValuesResolveKnownCoapClasses() {
    assertEquals(Clazz.REQUEST, Clazz.valueOf(0));
    assertEquals(Clazz.SUCCESS, Clazz.valueOf(2));
    assertEquals(Clazz.ERROR, Clazz.valueOf(4));
    assertEquals(Clazz.SERVER_ERROR, Clazz.valueOf(5));
    assertEquals(Clazz.SIGNAL, Clazz.valueOf(7));
  }

  @Test
  void unsupportedClassValueReturnsNull() {
    assertNull(Clazz.valueOf(1));
    assertNull(Clazz.valueOf(6));
  }

  @Test
  void stringRepresentationCarriesWireValueAndMeaning() {
    String value = Clazz.SERVER_ERROR.toString();
    assertTrue(value.contains("5"));
    assertTrue(value.contains("ServerError"));
    assertTrue(value.contains("Server error response packet"));
  }
}
