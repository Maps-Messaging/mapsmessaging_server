package io.mapsmessaging.state.config.geospatial;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoSpatialConfigSupportTest {

  @Test
  void singleAreaAndBoundaryConfigurationIsParsed() {
    ConfigurationProperties boundary = boundary("harbour", "/tmp/harbour.geojson", "do_not_enter");
    ConfigurationProperties area = new ConfigurationProperties();
    area.put("name", "Sesimbra");
    area.put("boundaries", boundary);
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("areas", area);

    GeoSpatialConfigDTO config = GeoSpatialConfigSupport.parse(root);

    assertEquals(1, config.getAreas().size());
    assertEquals("Sesimbra", config.getAreas().getFirst().getName());
    assertEquals(GeoSpatialBoundaryType.DO_NOT_ENTER, config.getAreas().getFirst().getBoundaries().getFirst().getType());
  }

  @Test
  void listRoundTripPreservesAreaBoundaryNamesPathsAndTypes() {
    GeoSpatialBoundaryConfigDTO boundary = new GeoSpatialBoundaryConfigDTO();
    boundary.setName("exercise-box");
    boundary.setPath("/tmp/exercise.geojson");
    boundary.setType(GeoSpatialBoundaryType.INSIDE);
    GeoSpatialAreaConfigDTO area = new GeoSpatialAreaConfigDTO();
    area.setName("REPMUS");
    area.setBoundaries(List.of(boundary));
    GeoSpatialConfigDTO source = new GeoSpatialConfigDTO();
    source.setAreas(List.of(area));

    GeoSpatialConfigDTO restored = GeoSpatialConfigSupport.parse(GeoSpatialConfigSupport.toConfigurationProperties(source));

    GeoSpatialBoundaryConfigDTO restoredBoundary = restored.getAreas().getFirst().getBoundaries().getFirst();
    assertEquals("REPMUS", restored.getAreas().getFirst().getName());
    assertEquals("/tmp/exercise.geojson", restoredBoundary.getPath());
    assertEquals(GeoSpatialBoundaryType.INSIDE, restoredBoundary.getType());
  }

  @Test
  void nullOrEmptyConfigurationPacksToEmptyProperties() {
    assertTrue(GeoSpatialConfigSupport.toConfigurationProperties(null).isEmpty());
    assertTrue(GeoSpatialConfigSupport.toConfigurationProperties(new GeoSpatialConfigDTO()).isEmpty());
  }

  private static ConfigurationProperties boundary(String name, String path, String type) {
    ConfigurationProperties boundary = new ConfigurationProperties();
    boundary.put("name", name);
    boundary.put("path", path);
    boundary.put("type", type);
    return boundary;
  }
}