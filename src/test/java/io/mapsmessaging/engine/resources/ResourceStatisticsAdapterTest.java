/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.storage.StorageStatistics;
import io.mapsmessaging.storage.impl.cache.CacheStatistics;
import io.mapsmessaging.storage.impl.tier.memory.MemoryTierStatistics;
import io.mapsmessaging.utilities.stats.Stats;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceStatisticsAdapterTest {

  @Test
  void cacheAdaptersForwardTheExpectedCounters() {
    CacheStatistics statistics = mock(CacheStatistics.class);
    when(statistics.getHit()).thenReturn(11L);
    when(statistics.getMiss()).thenReturn(7L);
    when(statistics.getSize()).thenReturn(19);

    Stats hit = mock(Stats.class);
    Stats miss = mock(Stats.class);
    Stats size = mock(Stats.class);

    new ResourceStatistics.CacheHitStats(hit).update(statistics);
    new ResourceStatistics.CacheMissStats(miss).update(statistics);
    new ResourceStatistics.CacheSizeStats(size).update(statistics);

    verify(hit).add(11L);
    verify(miss).add(7L);
    verify(size).add(19L);
  }

  @Test
  void tierAdaptersForwardMemoryAndMigrationCounters() {
    StorageStatistics memory = mock(StorageStatistics.class);
    when(memory.getReads()).thenReturn(13L);
    when(memory.getWrites()).thenReturn(17L);
    when(memory.getDeletes()).thenReturn(5L);
    when(memory.getTotalSize()).thenReturn(4096L);

    MemoryTierStatistics statistics = mock(MemoryTierStatistics.class);
    when(statistics.getMemoryStatistics()).thenReturn(memory);
    when(statistics.getMigratedCount()).thenReturn(23L);

    Stats reads = mock(Stats.class);
    Stats writes = mock(Stats.class);
    Stats deletes = mock(Stats.class);
    Stats migrations = mock(Stats.class);
    Stats size = mock(Stats.class);

    new ResourceStatistics.TierReadStats(reads).update(statistics);
    new ResourceStatistics.TierWriteStats(writes).update(statistics);
    new ResourceStatistics.TierDeleteStats(deletes).update(statistics);
    new ResourceStatistics.TierMigrationStats(migrations).update(statistics);
    new ResourceStatistics.TierSizeStats(size).update(statistics);

    verify(reads).add(13L);
    verify(writes).add(17L);
    verify(deletes).add(5L);
    verify(migrations).add(23L);
    verify(size).add(4096L);
  }
}
