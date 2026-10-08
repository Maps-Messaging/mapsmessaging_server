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
  /** CSD01 MQTT-SN 4.11 AUTH completes before 3.1.2 Will/session setup. */
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
  /** CSD01 MQTT-SN 4.14.2 uses PINGREQ to wake a sleeping session. */
  void sleepingClientWakesWithPingRequestAndOnlyExchangesAllowedPackets() throws Exception {
    MqttSn2Lifecycle lifecycle = new MqttSn2Lifecycle();
    lifecycle.begin(false, false);
    lifecycle.connected();
    lifecycle.sleep();
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.PUBLISH));
    lifecycle.checkAllowed(MqttSn2PacketType.PINGREQ);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.CONNECT));
    lifecycle.wake();
    lifecycle.checkAllowed(MqttSn2PacketType.PUBACK);
    assertThrows(IOException.class, () -> lifecycle.checkAllowed(MqttSn2PacketType.SUBSCRIBE));
    lifecycle.sleepAgain();
    assertEquals(MqttSn2Lifecycle.State.ASLEEP, lifecycle.state());
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
