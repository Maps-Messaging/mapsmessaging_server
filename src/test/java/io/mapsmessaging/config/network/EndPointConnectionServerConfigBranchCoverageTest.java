package io.mapsmessaging.config.network;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EndPointConnectionServerConfigBranchCoverageTest {

  @Test
  void singleLinkObjectLoadsAndSerializesAsLinkList() {
    ConfigurationProperties link = link("/remote/#", "/local");
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("links", link);
    properties.put("transformation", "Schema-To-Json");
    properties.put("plugin", true);
    properties.put("cost", 7);
    properties.put("groupName", "uplink");

    EndPointConnectionServerConfig config = new EndPointConnectionServerConfig(properties);

    assertEquals(1, config.getLinkConfigs().size());
    assertTrue(config.isPluginConnection());
    assertEquals(7, config.getCost());
    assertEquals("uplink", config.getGroupName());

    ConfigurationProperties packed = config.toConfigurationProperties();
    assertInstanceOf(List.class, packed.get("links"));
    assertEquals(1, ((List<?>) packed.get("links")).size());
  }

  @Test
  void linkListLoadsEveryEntry() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("links", List.of(
        link("/a/#", "/a"),
        link("/b/#", "/b")));

    EndPointConnectionServerConfig config = new EndPointConnectionServerConfig(properties);

    assertEquals(2, config.getLinkConfigs().size());
  }

  @Test
  void unrelatedDtoIsIgnored() {
    assertFalse(new EndPointConnectionServerConfig(new ConfigurationProperties()).update(new BaseConfigDTO()));
  }

  private static ConfigurationProperties link(String remote, String local) {
    ConfigurationProperties link = new ConfigurationProperties();
    link.put("direction", "pull");
    link.put("remote_namespace", remote);
    link.put("local_namespace", local);
    return link;
  }
}