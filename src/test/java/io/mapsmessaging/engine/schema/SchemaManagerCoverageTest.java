/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.engine.schema;

import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaResource;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaManagerCoverageTest {

  @ParameterizedTest(name = "{0} resolves default schema {1}")
  @MethodSource("defaultSchemaCases")
  void defaultSchemaLookupIsCaseInsensitiveAndFallsBackToRaw(String requested, UUID expectedId) throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    Map<String, SchemaResource> resources = defaultResources();
    resources.forEach((id, resource) -> when(repository.getResource(id)).thenReturn(resource));
    SchemaManager manager = manager(repository);

    SchemaConfig result = manager.getDefaultSchemaByName(requested);

    assertSame(resources.get(expectedId.toString()).getDefaultVersion(), result);
  }

  @ParameterizedTest(name = "{0} matches by {1}")
  @MethodSource("nameLookupCases")
  void nameLookupSupportsNameIdAndSuffix(String requested, MatchMode mode) throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaConfig alpha = schema("alpha.schema", "id-alpha", "json");
    SchemaConfig beta = schema("namespace.beta", "id-beta", "xml");
    SchemaResource alphaResource = resource(alpha);
    SchemaResource betaResource = resource(beta);
    when(repository.getAllSchemas()).thenReturn(List.of(alphaResource, betaResource));
    SchemaManager manager = manager(repository);

    SchemaConfig result = manager.getSchemaByName(requested);

    SchemaConfig expected = switch (mode) {
      case ALPHA -> alpha;
      case BETA -> beta;
      case NONE -> null;
    };
    assertSame(expected, result);
  }

  @ParameterizedTest(name = "type {0} selects matching formats")
  @MethodSource("typeLookupCases")
  void typeLookupReturnsOnlyMatchingDefaultSchemas(String type, int expectedCount) throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    List<SchemaResource> resources = List.of(
        resource(schema("one", "1", "json")),
        resource(schema("two", "2", "json")),
        resource(schema("three", "3", "xml")),
        resource(schema("four", "4", "protobuf"))
    );
    when(repository.getAllSchemas()).thenReturn(resources);
    SchemaManager manager = manager(repository);

    List<SchemaResource> result = manager.getSchemas(type);

    assertEquals(expectedCount, result.size());
    assertTrue(result.stream().allMatch(r -> type.equals(r.getDefaultVersion().getFormat())));
  }

  @ParameterizedTest(name = "context {0} returns {1} title matches")
  @MethodSource("contextLookupCases")
  void contextLookupMatchesTitlesBeforeResourceIdFallback(String context, int expectedTitleMatches) throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaResource alpha1 = resource(schemaWithTitle("a1", "id-a1", "json", "alpha"));
    SchemaResource alpha2 = resource(schemaWithTitle("a2", "id-a2", "xml", "alpha"));
    SchemaResource beta = resource(schemaWithTitle("b", "id-b", "json", "beta"));
    when(repository.getAllSchemas()).thenReturn(List.of(alpha1, alpha2, beta));
    when(repository.getResource(context)).thenReturn(beta);
    SchemaManager manager = manager(repository);

    List<SchemaResource> result = manager.getSchemaByContext(context);

    if (expectedTitleMatches > 0) {
      assertEquals(expectedTitleMatches, result.size());
      assertTrue(result.stream().allMatch(r -> context.equals(r.getDefaultVersion().getTitle())));
    } else {
      assertEquals(List.of(beta), result);
    }
  }

  @ParameterizedTest(name = "schema id {0} lookup")
  @MethodSource("idLookupCases")
  void schemaLookupReturnsDefaultVersionOrNull(String id, boolean present) throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaConfig config = schema("name", id, "json");
    SchemaResource configuredResource = present ? resource(config) : null;
    when(repository.getResource(id)).thenReturn(configuredResource);
    SchemaManager manager = manager(repository);

    SchemaConfig result = manager.getSchema(id);

    assertEquals(present, result != null);
    if (present) {
      assertSame(config, result);
    }
  }

  @Test
  void getAllSkipsResourcesWithoutDefaultVersionAndPreservesOrder() throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaConfig first = schema("first", "1", "json");
    SchemaConfig second = schema("second", "2", "xml");
    SchemaResource missing = mock(SchemaResource.class);
    SchemaResource firstResource = resource(first);
    SchemaResource secondResource = resource(second);
    when(repository.getAllSchemas()).thenReturn(List.of(firstResource, missing, secondResource));
    SchemaManager manager = manager(repository);

    assertEquals(List.of(first, second), manager.getAll());
  }

  @Test
  void schemaByTypeSkipsResourcesWithoutDefaultVersion() throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaConfig first = schema("first", "1", "json");
    SchemaResource missing = mock(SchemaResource.class);
    SchemaResource firstResource = resource(first);
    when(repository.search(eq("json"), anyMap(), eq(0), eq(0))).thenReturn(List.of(firstResource, missing));
    SchemaManager manager = manager(repository);

    assertEquals(List.of(first), manager.getSchemaByType("json"));
  }

  @Test
  void resolveParentReturnsDefaultVersionOrNull() throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaConfig child = mock(SchemaConfig.class);
    SchemaConfig parent = schema("parent", "parent-id", "json");
    when(child.getParentUuid()).thenReturn("parent-id");
    SchemaResource parentResource = resource(parent);
    when(repository.getResource("parent-id")).thenReturn(parentResource);
    SchemaManager manager = manager(repository);

    assertSame(parent, manager.resolveParent(child));

    when(repository.getResource("parent-id")).thenReturn(null);
    assertNull(manager.resolveParent(child));
  }

  @Test
  void mappedSchemasAreExposedAndStatusCountsMappedPaths() throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaManager manager = manager(repository);
    Map<String, List<SchemaConfig>> pathMap = field(manager, "pathMap");
    pathMap.put("/a", List.of(schema("a", "1", "json")));
    pathMap.put("/b", List.of(schema("b", "2", "xml")));

    assertSame(pathMap, manager.getMappedSchemas());
    assertEquals("Schema Manager", manager.getName());
    assertEquals("Manages the life cycle of schemas on the server", manager.getDescription());
    assertEquals(Status.OK, manager.getStatus().getStatus());
    assertTrue(manager.getStatus().getComment().contains("2"));
  }

  @Test
  void removeSchemaDeletesResourceClearsFormatterAndIncrementsUpdateCount() throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaManager manager = manager(repository);
    Map<String, Object> loadedFormatter = field(manager, "loadedFormatter");
    loadedFormatter.put("schema-id", mock(io.mapsmessaging.schemas.formatters.MessageFormatter.class));
    long before = manager.getUpdateCount();

    manager.removeSchema("schema-id");

    verify(repository).deleteResource("schema-id");
    assertFalse(loadedFormatter.containsKey("schema-id"));
    assertEquals(before + 1, manager.getUpdateCount());
  }

  @Test
  void messageFormatterWithNullConfigReturnsNullWithoutRepositoryLookup() throws Exception {
    SchemaRepository repository = mock(SchemaRepository.class);
    SchemaManager manager = manager(repository);

    assertNull(manager.getMessageFormatter((SchemaConfig) null));
    verifyNoInteractions(repository);
  }

  private static Stream<Arguments> defaultSchemaCases() {
    return Stream.of(
        Arguments.of("json", SchemaManager.DEFAULT_JSON_SCHEMA),
        Arguments.of("JSON", SchemaManager.DEFAULT_JSON_SCHEMA),
        Arguments.of("raw", SchemaManager.DEFAULT_RAW_UUID),
        Arguments.of("RAW", SchemaManager.DEFAULT_RAW_UUID),
        Arguments.of("numeric", SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA),
        Arguments.of("NUMERIC", SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA),
        Arguments.of("string", SchemaManager.DEFAULT_STRING_SCHEMA),
        Arguments.of("STRING", SchemaManager.DEFAULT_STRING_SCHEMA),
        Arguments.of("xml", SchemaManager.DEFAULT_XML_SCHEMA),
        Arguments.of("XML", SchemaManager.DEFAULT_XML_SCHEMA),
        Arguments.of("unknown", SchemaManager.DEFAULT_RAW_UUID),
        Arguments.of("", SchemaManager.DEFAULT_RAW_UUID),
        Arguments.of("protobuf", SchemaManager.DEFAULT_RAW_UUID)
    );
  }

  private static Stream<Arguments> nameLookupCases() {
    return Stream.of(
        Arguments.of("alpha.schema", MatchMode.ALPHA),
        Arguments.of("ALPHA.SCHEMA", MatchMode.ALPHA),
        Arguments.of("id-alpha", MatchMode.ALPHA),
        Arguments.of("ID-ALPHA", MatchMode.ALPHA),
        Arguments.of("schema", MatchMode.ALPHA),
        Arguments.of("namespace.beta", MatchMode.BETA),
        Arguments.of("NAMESPACE.BETA", MatchMode.BETA),
        Arguments.of("id-beta", MatchMode.BETA),
        Arguments.of("ID-BETA", MatchMode.BETA),
        Arguments.of("beta", MatchMode.BETA),
        Arguments.of("missing", MatchMode.NONE),
        Arguments.of("gamma", MatchMode.NONE)
    );
  }

  private static Stream<Arguments> typeLookupCases() {
    return Stream.of(
        Arguments.of("json", 2),
        Arguments.of("xml", 1),
        Arguments.of("protobuf", 1),
        Arguments.of("avro", 0),
        Arguments.of("", 0),
        Arguments.of("JSON", 0)
    );
  }

  private static Stream<Arguments> contextLookupCases() {
    return Stream.of(
        Arguments.of("alpha", 2),
        Arguments.of("beta", 1),
        Arguments.of("id-b", 0),
        Arguments.of("missing-id", 0)
    );
  }

  private static Stream<Arguments> idLookupCases() {
    return Stream.of(
        Arguments.of("id-1", true),
        Arguments.of("id-2", true),
        Arguments.of("id-3", false),
        Arguments.of("missing", false),
        Arguments.of("", false)
    );
  }

  private static Map<String, SchemaResource> defaultResources() {
    Map<String, SchemaResource> resources = new HashMap<>();
    for (UUID id : List.of(
        SchemaManager.DEFAULT_JSON_SCHEMA,
        SchemaManager.DEFAULT_RAW_UUID,
        SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA,
        SchemaManager.DEFAULT_STRING_SCHEMA,
        SchemaManager.DEFAULT_XML_SCHEMA)) {
      SchemaConfig config = schema(id.toString(), id.toString(), "json");
      resources.put(id.toString(), resource(config));
    }
    return resources;
  }

  private static SchemaConfig schema(String name, String id, String format) {
    return schemaWithTitle(name, id, format, name);
  }

  private static SchemaConfig schemaWithTitle(String name, String id, String format, String title) {
    SchemaConfig config = mock(SchemaConfig.class);
    when(config.getName()).thenReturn(name);
    when(config.getUniqueId()).thenReturn(id);
    when(config.getFormat()).thenReturn(format);
    when(config.getTitle()).thenReturn(title);
    return config;
  }

  private static SchemaResource resource(SchemaConfig config) {
    SchemaResource resource = mock(SchemaResource.class);
    when(resource.getDefaultVersion()).thenReturn(config);
    return resource;
  }

  private static SchemaManager manager(SchemaRepository repository) throws Exception {
    SchemaManager manager = mock(SchemaManager.class, CALLS_REAL_METHODS);
    setField(manager, "repository", repository);
    setField(manager, "loadedFormatter", new LinkedHashMap<String, Object>());
    setField(manager, "pathMap", new LinkedHashMap<String, List<SchemaConfig>>());
    setField(manager, "preLoadedSchemas", new HashMap<String, SchemaConfig>());
    return manager;
  }

  @SuppressWarnings("unchecked")
  private static <T> T field(SchemaManager manager, String name) throws Exception {
    Field field = SchemaManager.class.getDeclaredField(name);
    field.setAccessible(true);
    return (T) field.get(manager);
  }

  private static void setField(SchemaManager manager, String name, Object value) throws Exception {
    Field field = SchemaManager.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(manager, value);
  }

  private enum MatchMode {
    ALPHA,
    BETA,
    NONE
  }
}
