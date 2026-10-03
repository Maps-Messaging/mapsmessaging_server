package io.mapsmessaging.analytics.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.analytics.impl.stats.Statistics;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.selector.IdentifierResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StatisticalAnalyserCoverageTest {

  @Test
  void metadataCreateAndEmptyFlushAreStable() throws Exception {
    StatisticalAnalyser analyser = new StatisticalAnalyser();
    assertEquals("stats", analyser.getName());
    assertEquals("Statistical Event Analyser", analyser.getDescription());
    assertDoesNotThrow(analyser::close);
    assertEquals("{}", json(analyser.flush()).toString());

    StatisticsConfigDTO config = new StatisticsConfigDTO();
    config.setStatisticName("Advanced");
    config.setEventCount(17);
    config.setIgnoreList(List.of("serial"));
    config.setKeyList(List.of("temperature"));
    Analyser created = analyser.create(config);
    StatisticalAnalyser copy = assertInstanceOf(StatisticalAnalyser.class, created);
    assertEquals("Advanced", field(copy, "defaultAnalyser"));
    assertEquals(17, field(copy, "eventCount"));
    assertEquals(List.of("serial"), field(copy, "ignoreList"));
    assertEquals(List.of("temperature"), field(copy, "entries"));
  }

  @ParameterizedTest
  @MethodSource("valueCases")
  void discoverySelectsStatisticFamily(Object value, String expectedClass) throws Exception {
    StatisticalAnalyser analyser = analyser(10, List.of(), List.of());
    ParsedObject resolver = mock(ParsedObject.class);
    when(resolver.getKeys()).thenReturn(List.of("value"));
    when(resolver.get("value")).thenReturn(value);
    load(analyser, resolver);

    Statistics statistic = statistics(analyser).get("value");
    if (expectedClass == null) {
      assertNull(statistic);
    } else {
      assertNotNull(statistic);
      assertEquals(expectedClass, statistic.getClass().getSimpleName());
    }
  }

  @ParameterizedTest
  @MethodSource("filterCases")
  void ignoreAndEntryFiltersControlDiscovery(
      List<String> ignore, List<String> entries, String key, boolean expected) throws Exception {
    StatisticalAnalyser analyser = analyser(10, ignore, entries);
    ParsedObject resolver = mock(ParsedObject.class);
    when(resolver.getKeys()).thenReturn(List.of(key));
    when(resolver.get(key)).thenReturn(42);
    load(analyser, resolver);
    assertEquals(expected, statistics(analyser).containsKey(key));
  }

  @ParameterizedTest
  @MethodSource("eventCounts")
  void eventCountControlsEmission(int eventCount) throws Exception {
    StatisticalAnalyser analyser = analyser(eventCount, List.of(), List.of("value"));
    injectFormatter(analyser, formatter(resolver(Map.of("value", 10))));
    Message event = event();
    for (int i = 1; i < eventCount; i++) {
      assertNull(analyser.ingest(event));
    }
    assertTrue(json(analyser.ingest(event)).has("value"));
  }

  @ParameterizedTest
  @MethodSource("parseFailureCounts")
  void parseFailureStillAdvancesWindow(int eventCount) throws Exception {
    StatisticalAnalyser analyser = analyser(eventCount, List.of(), List.of());
    MessageFormatter formatter = mock(MessageFormatter.class);
    when(formatter.parse(any(byte[].class), any()))
        .thenThrow(new ParseException("bad payload"));
    injectFormatter(analyser, formatter);
    for (int i = 1; i < eventCount; i++) {
      assertNull(analyser.ingest(event()));
    }
    assertEquals("{}", json(analyser.ingest(event())).toString());
  }

  @Test
  void nullValueIncrementsMismatch() throws Exception {
    StatisticalAnalyser analyser = analyser(1, List.of(), List.of("value"));
    ParsedObject resolver = mock(ParsedObject.class);
    when(resolver.getKeys()).thenReturn(List.of("value"));
    when(resolver.get("value")).thenReturn(10, null);
    injectFormatter(analyser, formatter(resolver));

    JsonObject value = json(analyser.ingest(event())).getAsJsonObject("value");
    assertEquals(0, value.get("count").getAsInt());
    assertEquals(1, value.get("mismatched").getAsInt());
  }

  @Test
  void supportedValuesAreUpdatedAndEmitted() throws Exception {
    StatisticalAnalyser analyser = analyser(1, List.of(), List.of());
    injectFormatter(analyser, formatter(resolver(Map.of("temperature", 23.5, "state", "RUNNING"))));

    JsonObject result = json(analyser.ingest(event()));
    assertEquals(1, result.getAsJsonObject("temperature").get("count").getAsInt());
    assertEquals(1, result.getAsJsonObject("state").get("totalCount").getAsInt());
  }

  @Test
  void unsupportedIgnoredAndUnselectedValuesAreExcluded() throws Exception {
    StatisticalAnalyser analyser =
        analyser(1, List.of("serial"), List.of("temperature", "unsupported"));
    injectFormatter(analyser, formatter(resolver(Map.of(
        "serial", "ABC", "temperature", 21, "humidity", 64, "unsupported", true))));

    JsonObject result = json(analyser.ingest(event()));
    assertTrue(result.has("temperature"));
    assertFalse(result.has("serial"));
    assertFalse(result.has("humidity"));
    assertFalse(result.has("unsupported"));
  }

  @Test
  void populatedStatisticsPreventRediscovery() throws Exception {
    StatisticalAnalyser analyser = analyser(10, List.of(), List.of());
    MessageFormatter formatter = mock(MessageFormatter.class);
    ParsedObject first = resolver(Map.of("alpha", 1));
    ParsedObject second = resolver(Map.of("beta", 2));
    when(formatter.parse(any(byte[].class), any())).thenReturn(first, second);
    injectFormatter(analyser, formatter);

    analyser.ingest(event());
    analyser.ingest(event());

    assertTrue(statistics(analyser).containsKey("alpha"));
    assertFalse(statistics(analyser).containsKey("beta"));
  }

  @Test
  void emptyDiscoveryRetriesOnNextEvent() throws Exception {
    StatisticalAnalyser analyser = analyser(10, List.of(), List.of());
    MessageFormatter formatter = mock(MessageFormatter.class);
    ParsedObject first = resolver(Map.of("flag", true));
    ParsedObject second = resolver(Map.of("value", 5));
    when(formatter.parse(any(byte[].class), any())).thenReturn(first, second);
    injectFormatter(analyser, formatter);

    analyser.ingest(event());
    assertTrue(statistics(analyser).isEmpty());
    analyser.ingest(event());
    assertTrue(statistics(analyser).containsKey("value"));
  }

  @Test
  void flushAndThresholdEmissionResetState() throws Exception {
    StatisticalAnalyser analyser = analyser(2, List.of(), List.of("value"));
    injectFormatter(analyser, formatter(resolver(Map.of("value", 9))));
    Message event = event();

    assertNull(analyser.ingest(event));
    JsonObject threshold = json(analyser.ingest(event)).getAsJsonObject("value");
    assertEquals(2, threshold.get("count").getAsInt());

    assertNull(analyser.ingest(event));
    JsonObject flushed = json(analyser.flush()).getAsJsonObject("value");
    JsonObject reset = json(analyser.flush()).getAsJsonObject("value");
    assertEquals(1, flushed.get("count").getAsInt());
    assertEquals(0, reset.get("count").getAsInt());
  }

  @Test
  void formatterReceivesEveryPayload() throws Exception {
    StatisticalAnalyser analyser = analyser(10, List.of(), List.of("value"));
    MessageFormatter formatter = formatter(resolver(Map.of("value", 3)));
    injectFormatter(analyser, formatter);
    Message event = event();

    analyser.ingest(event);
    analyser.ingest(event);
    analyser.ingest(event);

    verify(formatter, times(3)).parse(any(byte[].class), any());
  }

  @Test
  void emptyKeySetProducesEmptyEvent() throws Exception {
    StatisticalAnalyser analyser = analyser(1, List.of(), List.of());
    ParsedObject resolver = mock(ParsedObject.class);
    when(resolver.getKeys()).thenReturn(List.of());
    injectFormatter(analyser, formatter(resolver));
    assertEquals("{}", json(analyser.ingest(event())).toString());
  }

  @Test
  void filteredKeysAreNotReadForDiscovery() throws Exception {
    StatisticalAnalyser analyser = analyser(10, List.of("ignored"), List.of("selected"));
    ParsedObject resolver = mock(ParsedObject.class);
    when(resolver.getKeys()).thenReturn(List.of("ignored", "selected", "other"));
    when(resolver.get("selected")).thenReturn(1);
    load(analyser, resolver);

    verify(resolver, never()).get("ignored");
    verify(resolver).get("selected");
    verify(resolver, never()).get("other");
  }

  private static StatisticalAnalyser analyser(
      int eventCount, List<String> ignore, List<String> entries) {
    return new StatisticalAnalyser("Advanced", eventCount, ignore, entries);
  }

  private static Message event() {
    Message event = mock(Message.class);
    when(event.getOpaqueData()).thenReturn(new byte[]{1, 2, 3});
    return event;
  }

  private static MessageFormatter formatter(ParsedObject resolver) throws ParseException {
    MessageFormatter formatter = mock(MessageFormatter.class);
    when(formatter.parse(any(byte[].class), any())).thenReturn(resolver);
    return formatter;
  }

  private static ParsedObject resolver(Map<String, Object> values) {
    ParsedObject resolver = mock(ParsedObject.class);
    when(resolver.getKeys()).thenReturn(List.copyOf(values.keySet()));
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      when(resolver.get(entry.getKey())).thenReturn(entry.getValue());
    }
    return resolver;
  }

  private static void injectFormatter(StatisticalAnalyser analyser, MessageFormatter formatter)
      throws Exception {
    Field field = StatisticalAnalyser.class.getDeclaredField("formatter");
    field.setAccessible(true);
    field.set(analyser, formatter);
  }

  private static void load(StatisticalAnalyser analyser, IdentifierResolver resolver)
      throws Exception {
    Method method =
        StatisticalAnalyser.class.getDeclaredMethod("loadEntries", IdentifierResolver.class);
    method.setAccessible(true);
    method.invoke(analyser, resolver);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Statistics> statistics(StatisticalAnalyser analyser) throws Exception {
    return (Map<String, Statistics>) field(analyser, "statistics");
  }

  private static Object field(StatisticalAnalyser analyser, String name) throws Exception {
    Field field = StatisticalAnalyser.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(analyser);
  }

  private static JsonObject json(Message message) {
    assertNotNull(message);
    return JsonParser.parseString(
        new String(message.getOpaqueData(), StandardCharsets.UTF_8)).getAsJsonObject();
  }

  private static Stream<Arguments> valueCases() {
    return Stream.of(
        Arguments.of("alpha", "StringStatistics"),
        Arguments.of("", "StringStatistics"),
        Arguments.of("123", "StringStatistics"),
        Arguments.of(1, "AdvancedStatistics"),
        Arguments.of(1L, "AdvancedStatistics"),
        Arguments.of(1.5d, "AdvancedStatistics"),
        Arguments.of(1.5f, "AdvancedStatistics"),
        Arguments.of((short) 2, "AdvancedStatistics"),
        Arguments.of((byte) 3, "AdvancedStatistics"),
        Arguments.of(new BigDecimal("4.2"), "AdvancedStatistics"),
        Arguments.of(new BigInteger("5"), "AdvancedStatistics"),
        Arguments.of(true, null),
        Arguments.of('x', null),
        Arguments.of(List.of(1), null),
        Arguments.of(Map.of("nested", 1), null),
        Arguments.of(new byte[]{1, 2}, null)
    );
  }

  private static Stream<Arguments> filterCases() {
    return Stream.of(
        Arguments.of(null, List.of(), "alpha", true),
        Arguments.of(List.of(), List.of(), "alpha", true),
        Arguments.of(List.of("alpha"), List.of(), "alpha", false),
        Arguments.of(List.of("other"), List.of(), "alpha", true),
        Arguments.of(null, List.of("alpha"), "alpha", true),
        Arguments.of(null, List.of("beta"), "alpha", false),
        Arguments.of(List.of("beta"), List.of("alpha"), "alpha", true),
        Arguments.of(List.of("alpha"), List.of("alpha"), "alpha", false),
        Arguments.of(List.of("alpha", "beta"), List.of("alpha", "beta"), "beta", false),
        Arguments.of(List.of("other"), List.of("alpha", "beta"), "beta", true),
        Arguments.of(List.of(), List.of("alpha", "beta"), "beta", true),
        Arguments.of(List.of(), List.of("alpha"), "beta", false)
    );
  }

  private static Stream<Arguments> eventCounts() {
    return Stream.of(Arguments.of(1), Arguments.of(2), Arguments.of(3), Arguments.of(5), Arguments.of(10));
  }

  private static Stream<Arguments> parseFailureCounts() {
    return Stream.of(Arguments.of(1), Arguments.of(2), Arguments.of(4));
  }
}
