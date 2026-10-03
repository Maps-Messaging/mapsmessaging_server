/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mavlink;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MavlinkProtocolAddressParsingTest {

  @Test
  void parsesSocketAddressFormsUsedByResponseCorrelation() throws Exception {
    Method parse = parser();

    InetSocketAddress plain = (InetSocketAddress) parse.invoke(null, "127.0.0.1:14550");
    assertEquals("127.0.0.1", plain.getHostString());
    assertEquals(14550, plain.getPort());

    InetSocketAddress slashPrefixed = (InetSocketAddress) parse.invoke(null, "/127.0.0.1:14551");
    assertEquals("127.0.0.1", slashPrefixed.getHostString());
    assertEquals(14551, slashPrefixed.getPort());

    InetSocketAddress hostAndResolvedAddress =
        (InetSocketAddress) parse.invoke(null, "vehicle.local/192.0.2.10:14552");
    assertEquals("vehicle.local", hostAndResolvedAddress.getHostString());
    assertEquals(14552, hostAndResolvedAddress.getPort());
  }

  @Test
  void rejectsMissingHostPortAndInvalidPort() throws Exception {
    Method parse = parser();

    assertCause(IllegalArgumentException.class, () -> parse.invoke(null, new Object[] {null}));
    assertCause(IllegalArgumentException.class, () -> parse.invoke(null, "   "));
    assertCause(IllegalArgumentException.class, () -> parse.invoke(null, "vehicle.local"));
    assertCause(IllegalArgumentException.class, () -> parse.invoke(null, "vehicle.local:"));
    assertCause(IllegalArgumentException.class, () -> parse.invoke(null, ":14550"));
    assertCause(NumberFormatException.class, () -> parse.invoke(null, "vehicle.local:not-a-port"));
  }

  @Test
  void hexDumpUsesUnsignedUppercaseBytesWithoutTrailingSpace() {
    assertEquals("", MavlinkProtocol.toHexDump(new byte[0]));
    assertEquals("00 7F 80 FF", MavlinkProtocol.toHexDump(new byte[] {0, 127, (byte) 128, (byte) 255}));
  }

  private Method parser() throws Exception {
    Method parse = MavlinkProtocol.class.getDeclaredMethod("parseSocketAddress", String.class);
    parse.setAccessible(true);
    return parse;
  }

  private void assertCause(Class<? extends Throwable> type, ThrowingAction action) {
    InvocationTargetException exception = assertThrows(InvocationTargetException.class, action::run);
    assertInstanceOf(type, exception.getCause());
  }

  private interface ThrowingAction {
    void run() throws Exception;
  }
}
