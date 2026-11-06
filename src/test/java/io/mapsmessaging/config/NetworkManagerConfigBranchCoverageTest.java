package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.NetworkManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkManagerConfigBranchCoverageTest {

  @Test
  void constructorHandlesMissingAndSingleEndpointData() throws Exception {
    NetworkManagerConfig empty = create(new ConfigurationProperties());
    assertTrue(empty.getEndPointServerConfigList().isEmpty());

    ConfigurationProperties endpoint = new ConfigurationProperties();
    endpoint.put("name", "edge");
    endpoint.put("url", "tcp://localhost:1883");
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("data", endpoint);

    NetworkManagerConfig single = create(root);
    assertEquals(1, single.getEndPointServerConfigList().size());
  }

  @Test
  void updateAppliesManagerScalarsAndListSizeChange() throws Exception {
    NetworkManagerConfig config = create(new ConfigurationProperties());
    NetworkManagerConfigDTO update = new NetworkManagerConfigDTO();
    update.setPreferIpV6Addresses(!config.isPreferIpV6Addresses());
    update.setScanNetworkChanges(!config.isScanNetworkChanges());
    update.setScanInterval(config.getScanInterval() + 1);
    update.setEndPointServerConfigList(List.of(new EndPointServerConfigDTO()));

    assertTrue(config.update(update));
    assertEquals(1, config.getEndPointServerConfigList().size());
  }

  @Test
  void unrelatedAndUnknownEndpointUpdatesAreRejected() throws Exception {
    NetworkManagerConfig config = create(new ConfigurationProperties());

    assertFalse(config.update(new BaseConfigDTO()));

    EndPointServerConfigDTO endpoint = new EndPointServerConfigDTO();
    endpoint.setName("missing");
    assertFalse(config.update(endpoint));
  }

  private static NetworkManagerConfig create(ConfigurationProperties properties) throws Exception {
    Constructor<NetworkManagerConfig> constructor =
        NetworkManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}