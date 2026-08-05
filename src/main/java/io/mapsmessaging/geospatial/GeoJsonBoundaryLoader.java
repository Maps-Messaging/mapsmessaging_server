/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.geospatial;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class GeoJsonBoundaryLoader {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private GeoJsonBoundaryLoader() {}

  public static GeoSpatialBoundary load(Path path, GeoSpatialBoundaryType type) throws IOException {
    Objects.requireNonNull(path, "path must not be null");
    try (InputStream inputStream = Files.newInputStream(path)) {
      return load(null, inputStream, path.getFileName().toString(), type);
    }
  }

  public static GeoSpatialBoundary load(
      String boundaryName, Path path, GeoSpatialBoundaryType type) throws IOException {
    Objects.requireNonNull(path, "path must not be null");
    try (InputStream inputStream = Files.newInputStream(path)) {
      return load(boundaryName, inputStream, path.getFileName().toString(), type);
    }
  }

  public static GeoSpatialBoundary load(
      InputStream inputStream, String sourceName, GeoSpatialBoundaryType type) throws IOException {
    return load(null, inputStream, sourceName, type);
  }

  public static GeoSpatialBoundary load(
      String boundaryName,
      InputStream inputStream,
      String sourceName,
      GeoSpatialBoundaryType type)
      throws IOException {
    Objects.requireNonNull(inputStream, "inputStream must not be null");
    Objects.requireNonNull(type, "type must not be null");
    String checkedSourceName = requireSourceName(sourceName);

    try {
      JsonNode root = OBJECT_MAPPER.readTree(inputStream);
      if (root == null || !root.isObject()) {
        throw new GeoJsonBoundaryException(checkedSourceName + " does not contain a GeoJSON object");
      }

      validateCoordinateReferenceSystem(root, checkedSourceName);
      List<GeoPolygon> polygons = new ArrayList<>();
      parseRoot(root, checkedSourceName, polygons);
      if (polygons.isEmpty()) {
        throw new GeoJsonBoundaryException(checkedSourceName + " contains no polygon geometry");
      }

      String resolvedName =
          boundaryName == null || boundaryName.isBlank()
              ? resolveBoundaryName(root, checkedSourceName)
              : boundaryName.trim();
      return new GeoSpatialBoundary(resolvedName, type, polygons);
    } catch (GeoJsonBoundaryException exception) {
      throw exception;
    } catch (IllegalArgumentException exception) {
      throw new GeoJsonBoundaryException(
          "Invalid geometry in " + checkedSourceName + ": " + exception.getMessage(), exception);
    }
  }

  private static void parseRoot(JsonNode node, String sourceName, List<GeoPolygon> polygons)
      throws GeoJsonBoundaryException {
    String type = requireText(node, "type", sourceName);
    switch (type) {
      case "FeatureCollection" -> parseFeatureCollection(node, sourceName, polygons);
      case "Feature" -> parseFeature(node, sourceName, polygons);
      case "Polygon", "MultiPolygon", "GeometryCollection" ->
          parseGeometry(node, sourceName, polygons);
      default ->
          throw new GeoJsonBoundaryException(
              sourceName + " has unsupported top-level GeoJSON type '" + type + "'");
    }
  }

  private static void parseFeatureCollection(
      JsonNode node, String sourceName, List<GeoPolygon> polygons)
      throws GeoJsonBoundaryException {
    JsonNode features = node.get("features");
    if (features == null || !features.isArray() || features.isEmpty()) {
      throw new GeoJsonBoundaryException(sourceName + " FeatureCollection contains no features");
    }
    for (int index = 0; index < features.size(); index++) {
      JsonNode feature = features.get(index);
      if (feature == null || !feature.isObject()) {
        throw new GeoJsonBoundaryException(
            sourceName + " feature " + index + " is not a GeoJSON object");
      }
      parseFeature(feature, sourceName + " feature " + index, polygons);
    }
  }

  private static void parseFeature(
      JsonNode node, String sourceName, List<GeoPolygon> polygons)
      throws GeoJsonBoundaryException {
    String type = requireText(node, "type", sourceName);
    if (!"Feature".equals(type)) {
      throw new GeoJsonBoundaryException(sourceName + " is not a GeoJSON Feature");
    }
    JsonNode geometry = node.get("geometry");
    if (geometry == null || geometry.isNull() || !geometry.isObject()) {
      throw new GeoJsonBoundaryException(sourceName + " has no geometry");
    }
    parseGeometry(geometry, sourceName + " geometry", polygons);
  }

  private static void parseGeometry(
      JsonNode node, String sourceName, List<GeoPolygon> polygons)
      throws GeoJsonBoundaryException {
    String type = requireText(node, "type", sourceName);
    switch (type) {
      case "Polygon" -> polygons.add(parsePolygon(requireCoordinates(node, sourceName), sourceName));
      case "MultiPolygon" -> parseMultiPolygon(requireCoordinates(node, sourceName), sourceName, polygons);
      case "GeometryCollection" -> parseGeometryCollection(node, sourceName, polygons);
      default ->
          throw new GeoJsonBoundaryException(
              sourceName + " contains unsupported geometry type '" + type + "'");
    }
  }

  private static void parseGeometryCollection(
      JsonNode node, String sourceName, List<GeoPolygon> polygons)
      throws GeoJsonBoundaryException {
    JsonNode geometries = node.get("geometries");
    if (geometries == null || !geometries.isArray() || geometries.isEmpty()) {
      throw new GeoJsonBoundaryException(sourceName + " GeometryCollection contains no geometries");
    }
    for (int index = 0; index < geometries.size(); index++) {
      JsonNode geometry = geometries.get(index);
      if (geometry == null || !geometry.isObject()) {
        throw new GeoJsonBoundaryException(
            sourceName + " geometry " + index + " is not a GeoJSON object");
      }
      parseGeometry(geometry, sourceName + " geometry " + index, polygons);
    }
  }

  private static void parseMultiPolygon(
      JsonNode coordinates, String sourceName, List<GeoPolygon> polygons)
      throws GeoJsonBoundaryException {
    if (coordinates.isEmpty()) {
      throw new GeoJsonBoundaryException(sourceName + " MultiPolygon contains no polygons");
    }
    for (int index = 0; index < coordinates.size(); index++) {
      polygons.add(parsePolygon(coordinates.get(index), sourceName + " polygon " + index));
    }
  }

  private static GeoPolygon parsePolygon(JsonNode coordinates, String sourceName)
      throws GeoJsonBoundaryException {
    if (coordinates == null || !coordinates.isArray() || coordinates.isEmpty()) {
      throw new GeoJsonBoundaryException(sourceName + " Polygon contains no rings");
    }

    List<GeoPoint> shellPoints = parseRing(coordinates.get(0), sourceName + " shell");
    List<List<GeoPoint>> holePoints = new ArrayList<>();
    for (int index = 1; index < coordinates.size(); index++) {
      holePoints.add(parseRing(coordinates.get(index), sourceName + " hole " + (index - 1)));
    }
    validatePolygon(shellPoints, holePoints, sourceName);

    GeoRing shell = new GeoRing(shellPoints);
    List<GeoRing> holes = holePoints.stream().map(GeoRing::new).toList();
    return new GeoPolygon(shell, holes);
  }

  private static List<GeoPoint> parseRing(JsonNode node, String sourceName)
      throws GeoJsonBoundaryException {
    if (node == null || !node.isArray() || node.size() < 4) {
      throw new GeoJsonBoundaryException(sourceName + " must contain at least four positions");
    }

    List<GeoPoint> points = new ArrayList<>(node.size());
    for (int index = 0; index < node.size(); index++) {
      points.add(parsePosition(node.get(index), sourceName + " position " + index));
    }
    if (!points.get(0).equals(points.get(points.size() - 1))) {
      throw new GeoJsonBoundaryException(sourceName + " is not closed");
    }
    return List.copyOf(points);
  }

  private static GeoPoint parsePosition(JsonNode node, String sourceName)
      throws GeoJsonBoundaryException {
    if (node == null || !node.isArray() || node.size() < 2) {
      throw new GeoJsonBoundaryException(sourceName + " must contain longitude and latitude");
    }
    JsonNode longitude = node.get(0);
    JsonNode latitude = node.get(1);
    if (!longitude.isNumber() || !latitude.isNumber()) {
      throw new GeoJsonBoundaryException(sourceName + " longitude and latitude must be numbers");
    }
    try {
      return new GeoPoint(latitude.doubleValue(), longitude.doubleValue());
    } catch (IllegalArgumentException exception) {
      throw new GeoJsonBoundaryException(sourceName + " is invalid: " + exception.getMessage(), exception);
    }
  }

  private static void validatePolygon(
      List<GeoPoint> shell, List<List<GeoPoint>> holes, String sourceName)
      throws GeoJsonBoundaryException {
    validateSimpleRing(shell, sourceName + " shell");
    GeoRing shellRing = new GeoRing(shell);

    List<GeoRing> validatedHoles = new ArrayList<>();
    for (int index = 0; index < holes.size(); index++) {
      List<GeoPoint> hole = holes.get(index);
      String holeName = sourceName + " hole " + index;
      validateSimpleRing(hole, holeName);
      if (shellRing.locate(hole.get(0)) != GeoLocation.INSIDE) {
        throw new GeoJsonBoundaryException(holeName + " is not strictly inside the polygon shell");
      }
      if (ringsIntersect(shell, hole)) {
        throw new GeoJsonBoundaryException(holeName + " intersects the polygon shell");
      }

      GeoRing holeRing = new GeoRing(hole);
      for (int otherIndex = 0; otherIndex < validatedHoles.size(); otherIndex++) {
        GeoRing otherHole = validatedHoles.get(otherIndex);
        List<GeoPoint> otherPoints = holes.get(otherIndex);
        if (ringsIntersect(otherPoints, hole)
            || otherHole.locate(hole.get(0)) != GeoLocation.OUTSIDE
            || holeRing.locate(otherPoints.get(0)) != GeoLocation.OUTSIDE) {
          throw new GeoJsonBoundaryException(
              holeName + " overlaps or contains polygon hole " + otherIndex);
        }
      }
      validatedHoles.add(holeRing);
    }
  }

  private static void validateSimpleRing(List<GeoPoint> ring, String sourceName)
      throws GeoJsonBoundaryException {
    double doubledArea = 0.0;
    for (int index = 1; index < ring.size(); index++) {
      GeoPoint previous = ring.get(index - 1);
      GeoPoint current = ring.get(index);
      if (previous.equals(current)) {
        throw new GeoJsonBoundaryException(sourceName + " contains consecutive duplicate positions");
      }
      doubledArea +=
          previous.longitude() * current.latitude() - current.longitude() * previous.latitude();
    }
    if (Math.abs(doubledArea) <= GeometryMath.EPSILON) {
      throw new GeoJsonBoundaryException(sourceName + " has zero area");
    }

    int edgeCount = ring.size() - 1;
    for (int firstEdge = 0; firstEdge < edgeCount; firstEdge++) {
      for (int secondEdge = firstEdge + 1; secondEdge < edgeCount; secondEdge++) {
        if (edgesAreAdjacent(firstEdge, secondEdge, edgeCount)) {
          continue;
        }
        List<Double> intersections = new ArrayList<>();
        GeometryMath.collectSegmentIntersections(
            ring.get(firstEdge),
            ring.get(firstEdge + 1),
            ring.get(secondEdge),
            ring.get(secondEdge + 1),
            intersections);
        if (!intersections.isEmpty()) {
          throw new GeoJsonBoundaryException(sourceName + " self-intersects");
        }
      }
    }
  }

  private static boolean ringsIntersect(List<GeoPoint> first, List<GeoPoint> second) {
    for (int firstEdge = 1; firstEdge < first.size(); firstEdge++) {
      for (int secondEdge = 1; secondEdge < second.size(); secondEdge++) {
        List<Double> intersections = new ArrayList<>();
        GeometryMath.collectSegmentIntersections(
            first.get(firstEdge - 1),
            first.get(firstEdge),
            second.get(secondEdge - 1),
            second.get(secondEdge),
            intersections);
        if (!intersections.isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean edgesAreAdjacent(int firstEdge, int secondEdge, int edgeCount) {
    return secondEdge == firstEdge + 1 || (firstEdge == 0 && secondEdge == edgeCount - 1);
  }

  private static JsonNode requireCoordinates(JsonNode node, String sourceName)
      throws GeoJsonBoundaryException {
    JsonNode coordinates = node.get("coordinates");
    if (coordinates == null || !coordinates.isArray()) {
      throw new GeoJsonBoundaryException(sourceName + " has no coordinate array");
    }
    return coordinates;
  }

  private static String requireText(JsonNode node, String fieldName, String sourceName)
      throws GeoJsonBoundaryException {
    JsonNode value = node.get(fieldName);
    if (value == null || !value.isTextual() || value.textValue().isBlank()) {
      throw new GeoJsonBoundaryException(
          sourceName + " has no valid '" + fieldName + "' property");
    }
    return value.textValue();
  }

  private static void validateCoordinateReferenceSystem(JsonNode root, String sourceName)
      throws GeoJsonBoundaryException {
    JsonNode crs = root.get("crs");
    if (crs == null || crs.isNull()) {
      return;
    }
    JsonNode name = crs.path("properties").get("name");
    if (name == null || !name.isTextual()) {
      throw new GeoJsonBoundaryException(sourceName + " has an unsupported CRS declaration");
    }

    String normalized = name.textValue().toUpperCase(Locale.ROOT);
    boolean crs84 = normalized.contains("CRS84");
    boolean epsg4326 = normalized.contains("EPSG") && normalized.endsWith("4326");
    if (!crs84 && !epsg4326) {
      throw new GeoJsonBoundaryException(
          sourceName + " uses unsupported coordinate reference system '" + name.textValue() + "'");
    }
  }

  private static String resolveBoundaryName(JsonNode root, String sourceName) {
    JsonNode name = root.get("name");
    if (name != null && name.isTextual() && !name.textValue().isBlank()) {
      return name.textValue().trim();
    }

    int separator = Math.max(sourceName.lastIndexOf('/'), sourceName.lastIndexOf('\\'));
    String filename = separator >= 0 ? sourceName.substring(separator + 1) : sourceName;
    int extension = filename.lastIndexOf('.');
    return extension > 0 ? filename.substring(0, extension) : filename;
  }

  private static String requireSourceName(String sourceName) {
    Objects.requireNonNull(sourceName, "sourceName must not be null");
    String trimmed = sourceName.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("sourceName must not be blank");
    }
    return trimmed;
  }
}
