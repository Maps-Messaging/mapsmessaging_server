package io.mapsmessaging.config.protocol;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LinkConfigTest {

  @Test
  void parsesNamespacesQosAndNamespaceFilters() {
    ConfigurationProperties properties = baseProperties();
    properties.put("qos", 5);
    ConfigurationProperties filter = new ConfigurationProperties();
    filter.put("namespace", "/remote/special/#");
    filter.put("filter", "priority > 3");
    filter.put("depth", 2);
    filter.put("forcePriority", true);
    properties.put("namespaceFilters", filter);

    LinkConfig config = new LinkConfig(properties);

    assertEquals(QualityOfService.EXACTLY_ONCE, config.getQualityOfService());
    assertEquals(1, config.getNamespaceFilters().size());
    assertEquals("priority > 3", config.getNamespaceFilters().getFirst().getSelector());
    assertTrue(config.getNamespaceFilters().getFirst().isForcePriority());
  }

  @Test
  void roundTripPreservesLinkSpecificProperties() {
    ConfigurationProperties properties = baseProperties();
    ConfigurationProperties linkProperties = new ConfigurationProperties();
    linkProperties.put("tenant", "public");
    linkProperties.put("namespace", "default");
    properties.put("linkProperties", linkProperties);

    LinkConfig source = new LinkConfig(properties);
    LinkConfig restored = new LinkConfig(source.toConfigurationProperties());

    assertEquals("public", restored.getLinkProperties().get("tenant"));
    assertEquals("default", restored.getLinkProperties().get("namespace"));
  }

  @Test
  void updateCopiesMutableLinkPropertiesAndThenReportsNoChange() {
    LinkConfig config = new LinkConfig(baseProperties());
    LinkConfigDTO updated = new LinkConfigDTO();
    updated.setDirection("push");
    updated.setRemoteNamespace("/new/remote/#");
    updated.setLocalNamespace("/new/local");
    updated.setSelector("enabled = true");
    updated.setIncludeSchema(true);
    updated.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
    updated.setTransformer(config.getTransformer());
    updated.setNamespaceFilters(config.getNamespaceFilters());
    updated.setStatistics(config.getStatistics());
    updated.setLinkProperties(new LinkedHashMap<>(Map.of("url", "pulsar://localhost:6650")));

    assertTrue(config.update(updated));
    assertEquals("push", config.getDirection());
    assertEquals("pulsar://localhost:6650", config.getLinkProperties().get("url"));
    assertFalse(config.update(updated));
    assertFalse(config.update(new BaseConfigDTO()));
  }

  private static ConfigurationProperties baseProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("direction", "pull");
    properties.put("remote_namespace", "/remote/#");
    properties.put("local_namespace", "/local");
    properties.put("selector", "");
    properties.put("include_schema", false);
    return properties;
  }
}