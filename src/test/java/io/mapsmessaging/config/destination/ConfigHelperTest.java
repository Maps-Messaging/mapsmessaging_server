package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.S3Config;
import io.mapsmessaging.dto.rest.config.destination.DeferredConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.MemoryStorageConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.PartitionStorageConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.StorageConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigHelperTest {

  @Test
  void memoryConfigurationUsesConfiguredLimitsAndDebugFlag() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("capacity", 500);
    properties.put("expiredEventPoll", 7);
    properties.put("debug", true);

    StorageConfigDTO result = ConfigHelper.buildConfig("memory", properties);

    MemoryStorageConfigDTO memory = assertInstanceOf(MemoryStorageConfigDTO.class, result);
    assertEquals(500, memory.getCapacity());
    assertEquals(7, memory.getExpiredEventPoll());
    assertTrue(memory.isDebug());
  }

  @Test
  void partitionRoundTripPreservesFilenameAndArchiveSettings() {
    PartitionStorageConfigDTO source = new PartitionStorageConfigDTO();
    source.setFileName("events.store");
    source.setCapacity(200);
    source.setExpiredEventPoll(9);
    source.setItemCount(64);
    source.setMaxPartitionSize(8192);
    source.setSync(true);
    source.setDebug(true);

    DeferredConfigDTO deferred = new DeferredConfigDTO();
    deferred.setDeferredName("S3");
    deferred.setIdleTime(1234);
    deferred.setDigestName("SHA-256");
    deferred.setMigrationDestination("/archive");
    S3Config s3 = new S3Config();
    s3.setRegion("eu-west-1");
    s3.setBucket("maps-test");
    s3.setAccessKey("key");
    s3.setSecretKey("secret");
    s3.setCompression(true);
    deferred.setS3Config(s3);
    source.setDeferredConfig(deferred);

    ConfigurationProperties packed = new ConfigurationProperties();
    ConfigHelper.packMap(packed, source);
    PartitionStorageConfigDTO restored =
        assertInstanceOf(PartitionStorageConfigDTO.class, ConfigHelper.buildConfig("partition", packed));

    assertEquals("events.store", restored.getFileName());
    assertEquals(200, restored.getCapacity());
    assertTrue(restored.isSync());
    assertEquals("S3", restored.getDeferredConfig().getDeferredName());
    assertEquals("maps-test", restored.getDeferredConfig().getS3Config().getBucket());
  }

  @Test
  void unsupportedStorageTypeIsRejectedWithoutInventingConfiguration() {
    assertNull(ConfigHelper.buildConfig("not-a-store", new ConfigurationProperties()));
  }
}
