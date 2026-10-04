/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.storage.StorageStatistics;
import io.mapsmessaging.storage.impl.cache.CacheStatistics;
import io.mapsmessaging.storage.impl.tier.memory.MemoryTierStatistics;
import io.mapsmessaging.utilities.stats.Stats;
import io.mapsmessaging.utilities.stats.StatsType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceStatisticsCoverageTest {

  @Test
  void disabledStatisticsDoNotInspectStorageAndExposeNoGroups() {
    Resource resource = mock(Resource.class);
    MessageDaemon daemon = mock(MessageDaemon.class);
    when(daemon.isEnableResourceStatistics()).thenReturn(false);

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      try (ResourceStatistics statistics = new ResourceStatistics(resource, StatsType.FULL)) {
        assertTrue(statistics.getStatistics().isEmpty());
        verifyNoInteractions(resource);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("statsTypes")
  void plainStorageCreatesOnlyStoreGroup(StatsType type) {
    Resource resource = mock(Resource.class);
    StorageStatistics storage = storage(11);
    when(resource.getStatistics()).thenReturn(storage);
    MessageDaemon daemon = enabledDaemon();

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      try (ResourceStatistics statistics = new ResourceStatistics(resource, type)) {
        Map<String, ?> groups = statistics.getStatistics();
        if (type == StatsType.FULL) {
          assertEquals(java.util.Set.of("store"), groups.keySet());
        } else {
          assertTrue(groups.containsKey("store"));
        }
        statistics.run();
      }
    }

    verifyStorageRead(storage);
  }

  @ParameterizedTest
  @MethodSource("statsTypes")
  void cacheStorageCreatesCacheAndStoreGroups(StatsType type) {
    Resource resource = mock(Resource.class);
    CacheStatistics cache = mock(CacheStatistics.class);
    StorageStatistics storage = storage(101);
    when(cache.getHit()).thenReturn(7L);
    when(cache.getMiss()).thenReturn(3L);
    when(cache.getSize()).thenReturn(5);
    when(cache.getStorageStatistics()).thenReturn(storage);
    when(resource.getStatistics()).thenReturn(cache);
    MessageDaemon daemon = enabledDaemon();

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      try (ResourceStatistics statistics = new ResourceStatistics(resource, type)) {
        assertEquals(java.util.Set.of("cache", "store"), statistics.getStatistics().keySet());
        statistics.run();
      }
    }

    verify(cache, atLeastOnce()).getHit();
    verify(cache, atLeastOnce()).getMiss();
    verify(cache, atLeastOnce()).getSize();
    verifyStorageRead(storage);
  }

  @ParameterizedTest
  @MethodSource("statsTypes")
  void memoryTierCreatesTierAndStoreGroups(StatsType type) {
    Resource resource = mock(Resource.class);
    MemoryTierStatistics tier = mock(MemoryTierStatistics.class);
    StorageStatistics memory = storage(201);
    StorageStatistics file = storage(301);
    when(tier.getMemoryStatistics()).thenReturn(memory);
    when(tier.getFileStatistics()).thenReturn(file);
    when(tier.getMigratedCount()).thenReturn(13L);
    when(resource.getStatistics()).thenReturn(tier);
    MessageDaemon daemon = enabledDaemon();

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      try (ResourceStatistics statistics = new ResourceStatistics(resource, type)) {
        assertEquals(java.util.Set.of("tier", "store"), statistics.getStatistics().keySet());
        statistics.run();
      }
    }

    verify(tier, atLeastOnce()).getMigratedCount();
    verify(memory, atLeastOnce()).getReads();
    verify(memory, atLeastOnce()).getWrites();
    verify(memory, atLeastOnce()).getDeletes();
    verify(memory, atLeastOnce()).getTotalSize();
    verifyStorageRead(file);
  }

  @ParameterizedTest
  @MethodSource("statsTypes")
  void cacheWrappingMemoryTierCreatesAllGroups(StatsType type) {
    Resource resource = mock(Resource.class);
    CacheStatistics cache = mock(CacheStatistics.class);
    MemoryTierStatistics tier = mock(MemoryTierStatistics.class);
    StorageStatistics memory = storage(401);
    StorageStatistics file = storage(501);
    when(cache.getStorageStatistics()).thenReturn(tier);
    when(cache.getHit()).thenReturn(4L);
    when(cache.getMiss()).thenReturn(2L);
    when(cache.getSize()).thenReturn(8);
    when(tier.getMemoryStatistics()).thenReturn(memory);
    when(tier.getFileStatistics()).thenReturn(file);
    when(tier.getMigratedCount()).thenReturn(17L);
    when(resource.getStatistics()).thenReturn(cache);
    MessageDaemon daemon = enabledDaemon();

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      try (ResourceStatistics statistics = new ResourceStatistics(resource, type)) {
        assertEquals(java.util.Set.of("cache", "tier", "store"), statistics.getStatistics().keySet());
        statistics.run();
      }
    }

    verify(cache, atLeastOnce()).getHit();
    verify(tier, atLeastOnce()).getMigratedCount();
    verifyStorageRead(file);
  }

  @Test
  void runtimeNullStatisticsAreIgnored() {
    Resource resource = mock(Resource.class);
    StorageStatistics initial = storage(601);
    when(resource.getStatistics()).thenReturn(initial).thenReturn(null);
    MessageDaemon daemon = enabledDaemon();

    try (MockedStatic<MessageDaemon> mocked = mockStatic(MessageDaemon.class)) {
      mocked.when(MessageDaemon::getInstance).thenReturn(daemon);

      try (ResourceStatistics statistics = new ResourceStatistics(resource, StatsType.FULL)) {
        assertDoesNotThrow(statistics::run);
      }
    }

    verify(resource, times(2)).getStatistics();
    verify(initial, never()).getReads();
  }

  @ParameterizedTest
  @MethodSource("cacheMetricCases")
  void cacheMetricAdaptersForwardExactCounter(
      java.util.function.Function<Stats, Object> adapterFactory,
      java.util.function.Consumer<CacheStatistics> update,
      long expected) {
    Stats movingAverage = mock(Stats.class);
    CacheStatistics cache = mock(CacheStatistics.class);
    when(cache.getHit()).thenReturn(11L);
    when(cache.getMiss()).thenReturn(12L);
    when(cache.getSize()).thenReturn(13);

    Object adapter = adapterFactory.apply(movingAverage);
    if (adapter instanceof ResourceStatistics.CacheHitStats stats) {
      stats.update(cache);
    } else if (adapter instanceof ResourceStatistics.CacheMissStats stats) {
      stats.update(cache);
    } else if (adapter instanceof ResourceStatistics.CacheSizeStats stats) {
      stats.update(cache);
    } else {
      fail("Unexpected adapter " + adapter);
    }

    verify(movingAverage).add(expected);
  }

  @ParameterizedTest
  @MethodSource("tierMetricCases")
  void tierMetricAdaptersForwardExactCounter(
      java.util.function.Function<Stats, Object> adapterFactory,
      long expected) {
    Stats movingAverage = mock(Stats.class);
    MemoryTierStatistics tier = mock(MemoryTierStatistics.class);
    StorageStatistics memory = mock(StorageStatistics.class);
    when(tier.getMemoryStatistics()).thenReturn(memory);
    when(memory.getReads()).thenReturn(21L);
    when(memory.getWrites()).thenReturn(22L);
    when(memory.getDeletes()).thenReturn(23L);
    when(memory.getTotalSize()).thenReturn(24L);
    when(tier.getMigratedCount()).thenReturn(25L);

    Object adapter = adapterFactory.apply(movingAverage);
    if (adapter instanceof ResourceStatistics.TierReadStats stats) {
      stats.update(tier);
    } else if (adapter instanceof ResourceStatistics.TierWriteStats stats) {
      stats.update(tier);
    } else if (adapter instanceof ResourceStatistics.TierDeleteStats stats) {
      stats.update(tier);
    } else if (adapter instanceof ResourceStatistics.TierSizeStats stats) {
      stats.update(tier);
    } else if (adapter instanceof ResourceStatistics.TierMigrationStats stats) {
      stats.update(tier);
    } else {
      fail("Unexpected adapter " + adapter);
    }

    verify(movingAverage).add(expected);
  }

  private static Stream<StatsType> statsTypes() {
    return Stream.of(StatsType.NONE, StatsType.BASIC, StatsType.FULL);
  }

  private static Stream<Arguments> cacheMetricCases() {
    return Stream.of(
        Arguments.of(
            (java.util.function.Function<Stats, Object>) ResourceStatistics.CacheHitStats::new,
            (java.util.function.Consumer<CacheStatistics>) ignored -> {},
            11L),
        Arguments.of(
            (java.util.function.Function<Stats, Object>) ResourceStatistics.CacheMissStats::new,
            (java.util.function.Consumer<CacheStatistics>) ignored -> {},
            12L),
        Arguments.of(
            (java.util.function.Function<Stats, Object>) ResourceStatistics.CacheSizeStats::new,
            (java.util.function.Consumer<CacheStatistics>) ignored -> {},
            13L)
    );
  }

  private static Stream<Arguments> tierMetricCases() {
    return Stream.of(
        Arguments.of((java.util.function.Function<Stats, Object>) ResourceStatistics.TierReadStats::new, 21L),
        Arguments.of((java.util.function.Function<Stats, Object>) ResourceStatistics.TierWriteStats::new, 22L),
        Arguments.of((java.util.function.Function<Stats, Object>) ResourceStatistics.TierDeleteStats::new, 23L),
        Arguments.of((java.util.function.Function<Stats, Object>) ResourceStatistics.TierSizeStats::new, 24L),
        Arguments.of((java.util.function.Function<Stats, Object>) ResourceStatistics.TierMigrationStats::new, 25L)
    );
  }

  private static MessageDaemon enabledDaemon() {
    MessageDaemon daemon = mock(MessageDaemon.class);
    when(daemon.isEnableResourceStatistics()).thenReturn(true);
    return daemon;
  }

  private static StorageStatistics storage(long base) {
    StorageStatistics storage = mock(StorageStatistics.class);
    when(storage.getReads()).thenReturn(base + 1);
    when(storage.getWrites()).thenReturn(base + 2);
    when(storage.getIops()).thenReturn(base + 3);
    when(storage.getDeletes()).thenReturn(base + 4);
    when(storage.getReadLatency()).thenReturn(base + 5);
    when(storage.getWriteLatency()).thenReturn(base + 6);
    when(storage.getBytesRead()).thenReturn(base + 7);
    when(storage.getBytesWritten()).thenReturn(base + 8);
    when(storage.getTotalSize()).thenReturn(base + 9);
    when(storage.getTotalEmptySpace()).thenReturn(base + 10);
    when(storage.getPartitionCount()).thenReturn((int) (base + 11));
    return storage;
  }

  private static void verifyStorageRead(StorageStatistics storage) {
    verify(storage, atLeastOnce()).getReads();
    verify(storage, atLeastOnce()).getWrites();
    verify(storage, atLeastOnce()).getIops();
    verify(storage, atLeastOnce()).getDeletes();
    verify(storage, atLeastOnce()).getReadLatency();
    verify(storage, atLeastOnce()).getWriteLatency();
    verify(storage, atLeastOnce()).getBytesRead();
    verify(storage, atLeastOnce()).getBytesWritten();
    verify(storage, atLeastOnce()).getTotalSize();
    verify(storage, atLeastOnce()).getTotalEmptySpace();
    verify(storage, atLeastOnce()).getPartitionCount();
  }
}
