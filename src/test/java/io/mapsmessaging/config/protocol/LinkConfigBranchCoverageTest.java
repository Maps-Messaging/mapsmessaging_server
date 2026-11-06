package io.mapsmessaging.config.protocol;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LinkConfigBranchCoverageTest {

  @Test
  void namespaceFilterListIgnoresUnsupportedEntriesAndAnalyticsBlockIsLoaded() {
    ConfigurationProperties filter = new ConfigurationProperties();
    filter.put("namespace", "/a/#");
    filter.put("filter", "");
    filter.put("depth", 1);
    filter.put("forcePriority", false);

    ConfigurationProperties analytics = new ConfigurationProperties();
    analytics.put("eventCount", 20);
    analytics.put("ignoreList", "");
    analytics.put("keyList", "");
    analytics.put("defaultAnalyser", "Base");

    ConfigurationProperties props = base();
    props.put("namespaceFilters", List.of(filter, "ignored"));
    props.put("analystics", analytics);

    LinkConfig config = new LinkConfig(props);

    assertEquals(1, config.getNamespaceFilters().size());
    assertNotNull(config.getStatistics());
  }

  @Test
  void updateCopiesMutableCollectionsAndConvertsNullLinkPropertiesToEmptyMap() {
    LinkConfig config = new LinkConfig(base());
    LinkConfigDTO update = new LinkConfigDTO();
    update.setDirection(config.getDirection());
    update.setRemoteNamespace(config.getRemoteNamespace());
    update.setLocalNamespace(config.getLocalNamespace());
    update.setSelector(config.getSelector());
    update.setIncludeSchema(config.isIncludeSchema());
    update.setQualityOfService(config.getQualityOfService());

    ArrayList<io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO> transformers =
        new ArrayList<>();
    update.setTransformer(transformers);
    ArrayList<io.mapsmessaging.dto.rest.config.protocol.NamespaceFilterDTO> filters =
        new ArrayList<>();
    update.setNamespaceFilters(filters);
    update.setStatistics(config.getStatistics());
    update.setLinkProperties(null);

    assertTrue(config.update(update));
    assertNotSame(transformers, config.getTransformer());
    assertNotSame(filters, config.getNamespaceFilters());
    assertNotNull(config.getLinkProperties());
    assertTrue(config.getLinkProperties().isEmpty());
  }

  @Test
  void serializationSkipsEmptyOptionalCollections() {
    LinkConfig config = new LinkConfig(base());

    ConfigurationProperties packed = config.toConfigurationProperties();

    assertFalse(packed.containsKey("transformer"));
    assertFalse(packed.containsKey("namespaceFilters"));
    assertFalse(packed.containsKey("linkProperties"));
  }

  private static ConfigurationProperties base() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("direction", "pull");
    props.put("remote_namespace", "/remote/#");
    props.put("local_namespace", "/local");
    props.put("selector", "");
    props.put("include_schema", false);
    return props;
  }
}