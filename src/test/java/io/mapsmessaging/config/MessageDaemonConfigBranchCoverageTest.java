package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class MessageDaemonConfigBranchCoverageTest {

  @Test
  void constructorReadsConfiguredScalarsAndRoundTripsCoreValues() throws Exception {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("DelayedPublishInterval", 25);
    props.put("SessionPipeLines", 7);
    props.put("TransactionExpiry", 1234L);
    props.put("TransactionScan", 2345L);
    props.put("CompressionName", "gzip");
    props.put("CompressMessageMinSize", 256);
    props.put("IncrementPriorityMethod", "linear");
    props.put("EnableResourceStatistics", true);
    props.put("EnableSystemTopics", true);
    props.put("EnableSystemStatusTopics", true);
    props.put("EnableSystemTopicAverages", true);
    props.put("EnableJMX", true);
    props.put("EnableJMXStatistics", true);
    props.put("tagMetaData", false);
    props.put("latitude", 38.4);
    props.put("longitude", -9.1);
    props.put("SendAnonymousStatusUpdates", true);
    props.put("exitOnConfigError", true);
    props.put("enableUniqueCorrelationIds", true);

    MessageDaemonConfig config = create(props);
    ConfigurationProperties packed = config.toConfigurationProperties();

    assertEquals(25, config.getDelayedPublishInterval());
    assertEquals(7, config.getSessionPipeLines());
    assertEquals("gzip", config.getCompressionName());
    assertTrue(config.isEnableJMX());
    assertTrue(config.isEnableUniqueCorrelationIds());
    assertEquals(38.4, packed.getDoubleProperty("latitude", 0), 0.0);
  }

  @Test
  void updateAppliesAllMutableFieldsAndRejectsUnrelatedDto() {
    MessageDaemonConfig config = new MessageDaemonConfig();
    assertFalse(config.update(new BaseConfigDTO()));

    MessageDaemonConfig update = new MessageDaemonConfig();
    update.setEnableUniqueCorrelationIds(!config.isEnableUniqueCorrelationIds());
    update.setDelayedPublishInterval(config.getDelayedPublishInterval() + 1);
    update.setSessionPipeLines(config.getSessionPipeLines() + 1);
    update.setTransactionExpiry(config.getTransactionExpiry() + 1);
    update.setTransactionScan(config.getTransactionScan() + 1);
    update.setCompressionName("changed");
    update.setCompressMessageMinSize(config.getCompressMessageMinSize() + 1);
    update.setIncrementPriorityMethod("changed");
    update.setSendAnonymousStatusUpdates(!config.isSendAnonymousStatusUpdates());
    update.setEnableResourceStatistics(!config.isEnableResourceStatistics());
    update.setEnableSystemTopics(!config.isEnableSystemTopics());
    update.setEnableSystemStatusTopics(!config.isEnableSystemStatusTopics());
    update.setEnableSystemTopicAverages(!config.isEnableSystemTopicAverages());
    update.setEnableJMX(!config.isEnableJMX());
    update.setEnableJMXStatistics(!config.isEnableJMXStatistics());
    update.setTagMetaData(!config.isTagMetaData());
    update.setLatitude(config.getLatitude() + 1);
    update.setLongitude(config.getLongitude() - 1);

    assertTrue(config.update(update));
    assertEquals("changed", config.getCompressionName());
    assertFalse(config.update(update));
  }

  private static MessageDaemonConfig create(ConfigurationProperties props) throws Exception {
    Constructor<MessageDaemonConfig> constructor =
        MessageDaemonConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(props);
  }
}