/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2LifecycleTest {

  @Test
  void clearConnectionOnlyBecomesUsableAfterSessionEstablishment() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.checkAllowed(MqttSn2PacketType.CONNECT);
    lifecycle.begin(false, false);
    assertEquals(MqttSn2Lifecycle.State.ESTABLISHING, lifecycle.state());
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH));
    lifecycle.connected();
    lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.CONNECT));
  }

  @Test
  void saslMustCompleteBeforeWillAndSessionEstablishment() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.begin(true, true);
    assertEquals(MqttSn2Lifecycle.State.AUTHENTICATING, lifecycle.state());
    lifecycle.checkAllowed(MqttSn2PacketType.AUTH);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.SUBSCRIBE));
    lifecycle.authenticated(true);
    assertEquals(MqttSn2Lifecycle.State.NEGOTIATING_WILL, lifecycle.state());
    assertThrows(IOException.class, () -> lifecycle.connected());
    lifecycle.willAccepted();
    lifecycle.connected();
    lifecycle.checkAllowed(MqttSn2PacketType.SUBSCRIBE);
  }

  @Test
  void sleepingClientOnlyAcceptsWakeupOrDisconnect() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.begin(false, false);
    lifecycle.connected();
    lifecycle.sleep();
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH));
    lifecycle.checkAllowed(MqttSn2PacketType.WAKEUP);
    lifecycle.wake();
    lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH);
  }

  @Test
  void closeIsTerminalAndInvalidTransitionsAreRejected() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    assertThrows(IOException.class, lifecycle::connected);
    lifecycle.close();
    assertThrows(IOException.class, () -> lifecycle.begin(false, false));
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.CONNECT));
  }
}
