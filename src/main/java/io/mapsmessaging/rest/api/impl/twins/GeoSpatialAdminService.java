/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.geospatial.GeoSpatialBoundaryType;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.geospatial.GeoSpatialAreaConfigDTO;
import io.mapsmessaging.state.config.geospatial.GeoSpatialBoundaryConfigDTO;
import io.mapsmessaging.state.config.geospatial.GeoSpatialConfigDTO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class GeoSpatialAdminService {

  private static final Path STORAGE_ROOT = Path.of(".", "conf", "geospatial").normalize();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final TwinManagerConfig config;

  GeoSpatialAdminService(TwinManagerConfig config) {
    this.config = Objects.requireNonNull(config, "config must not be null");
  }

  List<GeoSpatialAreaConfigDTO> listAreas() {
    return List.copyOf(geospatial().getAreas());
  }

  Optional<GeoSpatialAreaConfigDTO> getArea(String name) {
    validateName(name, "area name");
    return geospatial().getAreas().stream().filter(area -> name.equals(area.getName())).findFirst();
  }

  GeoSpatialAreaConfigDTO createArea(String name) throws IOException {
    validateName(name, "area name");
    synchronized (config) {
      if (getArea(name).isPresent()) {
        throw new TwinConfigurationStore.TwinConfigurationException("Geospatial area already exists: " + name, 409);
      }
      GeoSpatialAreaConfigDTO area = new GeoSpatialAreaConfigDTO();
      area.setName(name);
      geospatial().getAreas().add(area);
      config.save();
      return area;
    }
  }

  void deleteArea(String name) throws IOException {
    validateName(name, "area name");
    synchronized (config) {
      for (DroneInfoDTO drone : config.getDroneInfo()) {
        if (name.equals(drone.getGeospatialArea())) {
          throw new TwinConfigurationStore.TwinConfigurationException("Geospatial area is assigned to drone " + drone.getName() + ": " + name, 409);
        }
      }
      GeoSpatialAreaConfigDTO area = getArea(name).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial area: " + name, 404));
      geospatial().getAreas().remove(area);
      config.save();
      deleteDirectoryIfEmpty(areaDirectory(name));
    }
  }

  GeoSpatialBoundaryConfigDTO putBoundary(String areaName, String boundaryName, GeoSpatialBoundaryType type, byte[] geoJson) throws IOException {
    validateName(areaName, "area name");
    validateName(boundaryName, "boundary name");
    if (geoJson == null || geoJson.length == 0) {
      throw new TwinConfigurationStore.TwinConfigurationException("GeoJSON file is required", 400);
    }
    validateGeoJson(geoJson);
    GeoSpatialBoundaryType effectiveType = type == null ? GeoSpatialBoundaryType.INSIDE : type;

    synchronized (config) {
      GeoSpatialAreaConfigDTO area = getArea(areaName).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial area: " + areaName, 404));
      Path directory = areaDirectory(areaName);
      Files.createDirectories(directory);
      Path target = directory.resolve(safeFileName(boundaryName) + ".geojson").normalize();
      ensureInsideStorage(target);
      Path temporary = Files.createTempFile(directory, safeFileName(boundaryName) + "-", ".tmp");
      Files.write(temporary, geoJson);
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

      GeoSpatialBoundaryConfigDTO boundary = area.getBoundaries().stream().filter(candidate -> boundaryName.equals(candidate.getName())).findFirst().orElseGet(() -> {
        GeoSpatialBoundaryConfigDTO created = new GeoSpatialBoundaryConfigDTO();
        created.setName(boundaryName);
        area.getBoundaries().add(created);
        return created;
      });
      boundary.setType(effectiveType);
      boundary.setPath(target.toString());
      config.save();
      return boundary;
    }
  }

  byte[] getBoundaryContent(String areaName, String boundaryName) throws IOException {
    GeoSpatialBoundaryConfigDTO boundary = requireBoundary(areaName, boundaryName);
    Path path = Path.of(boundary.getPath()).normalize();
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      throw new TwinConfigurationStore.TwinConfigurationException("GeoJSON boundary file does not exist: " + boundary.getPath(), 404);
    }
    return Files.readAllBytes(path);
  }

  void deleteBoundary(String areaName, String boundaryName) throws IOException {
    synchronized (config) {
      GeoSpatialAreaConfigDTO area = getArea(areaName).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial area: " + areaName, 404));
      GeoSpatialBoundaryConfigDTO boundary = area.getBoundaries().stream().filter(candidate -> boundaryName.equals(candidate.getName())).findFirst().orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial boundary: " + boundaryName, 404));
      area.getBoundaries().remove(boundary);
      config.save();
      if (boundary.getPath() != null && !boundary.getPath().isBlank()) {
        Path path = Path.of(boundary.getPath()).normalize();
        if (path.startsWith(STORAGE_ROOT)) {
          Files.deleteIfExists(path);
        }
      }
      deleteDirectoryIfEmpty(areaDirectory(areaName));
    }
  }

  private GeoSpatialBoundaryConfigDTO requireBoundary(String areaName, String boundaryName) {
    return getArea(areaName).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial area: " + areaName, 404)).getBoundaries().stream().filter(candidate -> boundaryName.equals(candidate.getName())).findFirst().orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown geospatial boundary: " + boundaryName, 404));
  }

  private GeoSpatialConfigDTO geospatial() {
    if (config.getGeospatial() == null) {
      config.setGeospatial(new GeoSpatialConfigDTO());
    }
    return config.getGeospatial();
  }

  private void validateGeoJson(byte[] content) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(content);
      if (root == null || !root.isObject()) {
        throw new TwinConfigurationStore.TwinConfigurationException("GeoJSON root must be a JSON object", 400);
      }
      String type = root.path("type").asText();
      if (!List.of("Feature", "FeatureCollection", "Polygon", "MultiPolygon").contains(type)) {
        throw new TwinConfigurationStore.TwinConfigurationException("Unsupported GeoJSON root type: " + type, 400);
      }
    } catch (TwinConfigurationStore.TwinConfigurationException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new TwinConfigurationStore.TwinConfigurationException("Invalid GeoJSON: " + exception.getMessage(), 400);
    }
  }

  private Path areaDirectory(String areaName) {
    Path result = STORAGE_ROOT.resolve(safeFileName(areaName)).normalize();
    ensureInsideStorage(result);
    return result;
  }

  private String safeFileName(String value) {
    String safe = value.replaceAll("[^A-Za-z0-9._-]", "_");
    if (safe.isBlank() || ".".equals(safe) || "..".equals(safe)) {
      throw new TwinConfigurationStore.TwinConfigurationException("Invalid geospatial name: " + value, 400);
    }
    return safe;
  }

  private void ensureInsideStorage(Path path) {
    if (!path.normalize().startsWith(STORAGE_ROOT)) {
      throw new TwinConfigurationStore.TwinConfigurationException("Invalid geospatial storage path", 400);
    }
  }

  private void deleteDirectoryIfEmpty(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      return;
    }
    try (var entries = Files.list(directory)) {
      if (entries.findAny().isEmpty()) {
        Files.deleteIfExists(directory);
      }
    }
  }

  private void validateName(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new TwinConfigurationStore.TwinConfigurationException(label + " is required", 400);
    }
  }
}
