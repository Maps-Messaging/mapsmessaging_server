package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Connect5BranchCoverageTest {

  @Test
  void defaultConnectHasNoCredentialsOrWill() {
    Connect5 connect = new Connect5();

    assertFalse(connect.hasUsername());
    assertFalse(connect.hasPassword());
    assertFalse(connect.isWillFlag());
    assertEquals(QualityOfService.AT_MOST_ONCE, connect.getWillQOS());
    assertEquals(5, connect.getProtocolLevel());
  }

  @Test
  void blankUsernameAndEmptyPasswordRemainAbsent() {
    Connect5 connect = new Connect5();
    connect.setUsername("  ");
    connect.setPassword(new char[0]);

    assertFalse(connect.hasUsername());
    assertFalse(connect.hasPassword());
  }

  @Test
  void packFrameIncludesWillCredentialsAndOptionalFlags() {
    Connect5 connect = new Connect5();
    connect.setSessionId("client");
    connect.setCleanSession(true);
    connect.setKeepAlive(30);
    connect.setUsername("user");
    connect.setPassword("pass".toCharArray());
    connect.setWillFlag(true);
    connect.setWillQOS(QualityOfService.AT_LEAST_ONCE);
    connect.setWillRetain(true);
    connect.setWillTopic("/last/will");
    connect.setWillMsg(new byte[]{1, 2, 3});
    connect.setWillProperties(new MessageProperties());

    Packet packet = new Packet(1024, false);
    int remaining = connect.packFrame(packet);

    assertTrue(remaining > 0);
    assertTrue(packet.position() > remaining);
    assertTrue(connect.toString().contains("username:user"));
    assertTrue(connect.toString().contains("Password Len:4"));
  }
}