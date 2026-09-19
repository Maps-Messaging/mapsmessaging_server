package io.mapsmessaging.api.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QualityOfServiceTest {

  @Test
  void numericLevelsResolveToProtocolSemantics() {
    assertSame(QualityOfService.AT_MOST_ONCE, QualityOfService.getInstance(0));
    assertSame(QualityOfService.AT_LEAST_ONCE, QualityOfService.getInstance(1));
    assertSame(QualityOfService.EXACTLY_ONCE, QualityOfService.getInstance(2));
    assertSame(QualityOfService.MQTT_SN_REGISTERED, QualityOfService.getInstance(3));
  }

  @Test
  void invalidLevelsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> QualityOfService.getInstance(-1));
    assertThrows(IllegalArgumentException.class, () -> QualityOfService.getInstance(4));
    assertThrows(IllegalArgumentException.class, () -> QualityOfService.getInstance(Integer.MAX_VALUE));
  }

  @Test
  void deliveryGuaranteesCarryExpectedStorageAndPacketIdRequirements() {
    assertFalse(QualityOfService.AT_MOST_ONCE.isStoreOffLine());
    assertFalse(QualityOfService.AT_MOST_ONCE.isSendPacketId());
    assertEquals(ClientAcknowledgement.AUTO,
        QualityOfService.AT_MOST_ONCE.getClientAcknowledgement());

    assertTrue(QualityOfService.AT_LEAST_ONCE.isStoreOffLine());
    assertTrue(QualityOfService.AT_LEAST_ONCE.isSendPacketId());
    assertEquals(ClientAcknowledgement.INDIVIDUAL,
        QualityOfService.AT_LEAST_ONCE.getClientAcknowledgement());

    assertTrue(QualityOfService.EXACTLY_ONCE.isStoreOffLine());
    assertTrue(QualityOfService.EXACTLY_ONCE.isSendPacketId());

    assertTrue(QualityOfService.MQTT_SN_REGISTERED.isStoreOffLine());
    assertFalse(QualityOfService.MQTT_SN_REGISTERED.isSendPacketId());
  }
}
