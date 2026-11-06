package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechTransmitDefaultsDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SemtechConfigBranchCoverageTest {

  @Test
  void missingTransmitBlockUsesDtoDefaultsAndStillSerializesTx() {
    SemtechConfig config = new SemtechConfig(new ConfigurationProperties());

    assertNotNull(config.getTransmitDefaults());
    assertTrue(config.toConfigurationProperties().containsKey("tx"));
  }

  @Test
  void updateCoversEveryTransmitDefaultAndNullSafeStringComparison() {
    SemtechConfig config = new SemtechConfig(new ConfigurationProperties());
    config.getTransmitDefaults().setModu(null);
    config.getTransmitDefaults().setDatr(null);
    config.getTransmitDefaults().setCodr(null);

    SemtechTransmitDefaultsDTO tx = new SemtechTransmitDefaultsDTO();
    tx.setImme(!config.getTransmitDefaults().isImme());
    tx.setFreq(config.getTransmitDefaults().getFreq() + 1.0);
    tx.setRfch(config.getTransmitDefaults().getRfch() + 1);
    tx.setPowe(config.getTransmitDefaults().getPowe() + 1);
    tx.setModu("LORA");
    tx.setDatr("SF7BW125");
    tx.setCodr("4/5");
    tx.setIpol(!config.getTransmitDefaults().isIpol());

    SemtechConfigDTO update = new SemtechConfigDTO();
    update.setMaxQueued(config.getMaxQueued());
    update.setInboundTopicName(config.getInboundTopicName());
    update.setOutboundTopicName(config.getOutboundTopicName());
    update.setStatusTopicName(config.getStatusTopicName());
    update.setTelemetryTopicName(config.getTelemetryTopicName());
    update.setTransmitDefaults(tx);

    assertTrue(config.update(update));
    assertEquals("LORA", config.getTransmitDefaults().getModu());
    assertEquals("SF7BW125", config.getTransmitDefaults().getDatr());
    assertEquals("4/5", config.getTransmitDefaults().getCodr());
  }

  @Test
  void unrelatedDtoIsRejected() {
    assertFalse(new SemtechConfig(new ConfigurationProperties()).update(new BaseConfigDTO()));
  }
}