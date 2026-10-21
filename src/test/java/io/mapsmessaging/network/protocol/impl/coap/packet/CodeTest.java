package io.mapsmessaging.network.protocol.impl.coap.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeTest {

  @Test
  void everyDefinedResponseCodeRoundTripsThroughItsWireValue() {
    for (Code code : Code.values()) {
      if (code == Code.EMPTY) {
        continue;
      }
      assertEquals(code, Code.valueOf(code.getValue()), code.name());
    }
  }

  @Test
  void unknownMinorCodesFallBackWithinTheirResponseClass() {
    assertEquals(Code.CREATED, Code.valueOf((byte) ((Clazz.SUCCESS.getValue() << 5) | 30)));
    assertEquals(Code.BAD_REQUEST, Code.valueOf((byte) ((Clazz.ERROR.getValue() << 5) | 31)));
    assertEquals(Code.INTERNAL_SERVER_ERROR, Code.valueOf((byte) ((Clazz.SERVER_ERROR.getValue() << 5) | 31)));
  }

  @Test
  void requestSignalAndUnknownClassesUseInternalServerErrorFallback() {
    assertEquals(Code.INTERNAL_SERVER_ERROR, Code.valueOf((byte) 0));
    assertEquals(Code.INTERNAL_SERVER_ERROR, Code.valueOf((byte) (Clazz.SIGNAL.getValue() << 5)));
    assertEquals(Code.INTERNAL_SERVER_ERROR, Code.valueOf((byte) (3 << 5)));
  }
}
