package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformationConfigFactoryBranchCoverageTest {

  @Test
  void nullChainIsEmptyAndSingleMapUsesLegacyNameToken() {
    assertTrue(TransformationConfigFactory.loadChain(null).isEmpty());

    TransformationConfigDTO config =
        TransformationConfigFactory.loadSingle(Map.of("name", "jsontoxml"));

    assertEquals(TransformationType.JSON_TO_XML, config.getType());
  }

  @Test
  void listChainLoadsMultipleTransformationTypes() {
    ConfigurationProperties geohash = new ConfigurationProperties();
    geohash.put("type", "geohash");
    ConfigurationProperties cloudEvent = new ConfigurationProperties();
    cloudEvent.put("type", "cloudevent-json");

    List<TransformationConfigDTO> chain =
        TransformationConfigFactory.loadChain(List.of(geohash, cloudEvent));

    assertEquals(2, chain.size());
    assertEquals(TransformationType.GEOHASH, chain.get(0).getType());
    assertEquals(TransformationType.CLOUD_EVENT_JSON, chain.get(1).getType());
  }

  @Test
  void missingUnknownNullAndUnsupportedEntriesAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TransformationConfigFactory.loadSingle(new ConfigurationProperties()));
    assertThrows(
        IllegalArgumentException.class,
        () -> TransformationConfigFactory.loadSingle(Map.of("type", "unknown")));
    assertThrows(
        IllegalArgumentException.class,
        () -> TransformationConfigFactory.loadSingle(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> TransformationConfigFactory.loadSingle("not-a-map"));
  }
}