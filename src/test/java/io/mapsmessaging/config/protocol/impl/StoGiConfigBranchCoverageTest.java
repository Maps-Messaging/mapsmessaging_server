package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.StoGiConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoGiConfigBranchCoverageTest {

  @Test
  void constructorReadsProtocolSpecificFieldsAndSerializesThem() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("initialSetup", "ATZ");
    props.put("incomingMessagePollInterval", 11);
    props.put("outgoingMessagePollInterval", 22);
    props.put("modemResponseTimeout", 33L);
    props.put("locationPollInterval", 44L);
    props.put("modemStatsTopic", "/stats");
    props.put("maxBufferSize", 55);
    props.put("compressionCutoffSize", 66);
    props.put("messageLifeTimeInMinutes", 77);
    props.put("sharedSecret", "secret");
    props.put("sendHighPriorityMessages", true);
    props.put("sinNumber", 88);

    StoGiConfig config = new StoGiConfig(props);
    ConfigurationProperties packed = config.toConfigurationProperties();

    assertEquals("ATZ", config.getInitialSetup());
    assertEquals("/stats", config.getModemStatsTopic());
    assertEquals(88, config.getSinNumber());
    assertEquals("secret", packed.getProperty("sharedSecret"));
  }

  @Test
  void updateAppliesProtocolSpecificChanges() {
    StoGiConfig config = new StoGiConfig(new ConfigurationProperties());
    StoGiConfigDTO update = new StoGiConfigDTO();
    update.setModemStatsTopic("/changed");
    update.setSinNumber(config.getSinNumber() + 1);
    update.setInitialSetup("AT+SETUP");
    update.setIncomingMessagePollInterval(config.getIncomingMessagePollInterval() + 1);
    update.setOutgoingMessagePollInterval(config.getOutgoingMessagePollInterval() + 1);
    update.setModemResponseTimeout(config.getModemResponseTimeout() + 1);
    update.setLocationPollInterval(config.getLocationPollInterval() + 1);
    update.setMaxBufferSize(config.getMaxBufferSize() + 1);
    update.setCompressionCutoffSize(config.getCompressionCutoffSize() + 1);
    update.setSendHighPriorityMessages(!config.isSendHighPriorityMessages());
    update.setMessageLifeTimeInMinutes(config.getMessageLifeTimeInMinutes() + 1);
    update.setSharedSecret("changed");

    assertTrue(config.update(update));
    assertEquals("/changed", config.getModemStatsTopic());
    assertEquals("AT+SETUP", config.getInitialSetup());
    assertEquals("changed", config.getSharedSecret());
    assertFalse(config.update(update));
  }

  @Test
  void unrelatedDtoIsRejected() {
    assertFalse(new StoGiConfig(new ConfigurationProperties()).update(new BaseConfigDTO()));
  }
}