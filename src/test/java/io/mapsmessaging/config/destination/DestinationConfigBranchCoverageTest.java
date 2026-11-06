package io.mapsmessaging.config.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DestinationConfigBranchCoverageTest {

  @Test
  void unsupportedFileStorageFallsBackToMemoryAndDisabledCacheIsIgnored() {
    ConfigurationProperties cache = new ConfigurationProperties();
    cache.put("type", "memory");
    cache.put("writeThrough", "enable");

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("directory", "./target/{folder}/messages");
    properties.put("namespace", "/fleet/{folder}");
    properties.put("type", "file");
    properties.put("cache", cache);

    DestinationConfig config =
        new DestinationConfig(properties, new FeatureManager(new ArrayList<>()));

    assertEquals("memory", config.getType());
    assertNull(config.getCache());
    assertTrue(config.isRemap());
    assertEquals("/fleet/", config.getNamespaceMapping());
    assertEquals("/messages", config.getTrailingPath());
  }

  @Test
  void remapRequiresPlaceholderInBothNamespaceAndDirectory() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("directory", "./target/messages");
    properties.put("namespace", "/fleet/{folder}");
    properties.put("type", "memory");

    DestinationConfig config =
        new DestinationConfig(properties, new FeatureManager(new ArrayList<>()));

    assertFalse(config.isRemap());
    assertEquals("/fleet/{folder}", config.getNamespaceMapping());
    assertEquals("", config.getTrailingPath());
  }

  @Test
  void updateRecomputesDerivedNamespaceMapping() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("directory", "./target/messages");
    properties.put("namespace", "/old");
    properties.put("type", "memory");
    DestinationConfig config =
        new DestinationConfig(properties, new FeatureManager(new ArrayList<>()));

    DestinationConfigDTO update = new DestinationConfigDTO();
    update.setDirectory("./target/{folder}/tail");
    update.setNamespace("/new/{folder}");
    update.setType("memory");
    update.setAutoPauseTimeout(config.getAutoPauseTimeout());
    update.setStorageConfig(config.getStorageConfig());

    assertTrue(config.update(update));
    assertTrue(config.isRemap());
    assertEquals("/new/", config.getNamespaceMapping());
    assertEquals("/tail", config.getTrailingPath());
  }
}