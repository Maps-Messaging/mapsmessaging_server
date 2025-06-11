/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.storage.StorageStatistics;
import io.mapsmessaging.storage.impl.cache.CacheStatistics;
import io.mapsmessaging.storage.impl.tier.memory.MemoryTierStatistics;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import io.mapsmessaging.utilities.stats.Statistics;
import io.mapsmessaging.utilities.stats.Stats;
import io.mapsmessaging.utilities.stats.StatsType;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ResourceStatistics extends Statistics implements AutoCloseable, Runnable {

  private final Future<?> future;
  private final Resource resource;
  private final List<StorageStats> storeStats;
  private final List<CacheStats> cacheStats;
  private final List<TierStats> tierStats;

  public ResourceStatistics(Resource resource, StatsType type) {
    storeStats = new ArrayList<>();
    cacheStats = new ArrayList<>();
    tierStats = new ArrayList<>();
    this.resource = resource;
    if(MessageDaemon.getInstance().isEnableResourceStatistics()) {
      future = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);

      io.mapsmessaging.storage.Statistics statistics = resource.getStatistics();
      if (statistics instanceof CacheStatistics) {
        cacheStats.add(new CacheHitStats(create(type,ACCUMULATOR.DIFF, "Cache Hits", "Hits/second")));
        cacheStats.add(new CacheMissStats(create(type,ACCUMULATOR.DIFF, "Cache Miss", "Hits/second")));
        cacheStats.add(new CacheSizeStats(create(type,ACCUMULATOR.DIFF, "Cache Size", "Entries")));
        statistics = ((CacheStatistics) statistics).getStorageStatistics();
      }

      if (statistics instanceof MemoryTierStatistics) {
        tierStats.add(new TierSizeStats(create(type, ACCUMULATOR.ADD, "Top Tier Entries", "Entries")));
        tierStats.add(new TierReadStats(create(type, ACCUMULATOR.ADD, "Top Tier Read Operations", "Reads/second")));
        tierStats.add(new TierWriteStats(create(type, ACCUMULATOR.ADD, "Top Tier Write Operations", "Writes/second")));
        tierStats.add(new TierDeleteStats(create(type, ACCUMULATOR.ADD, "Top Tier Delete Operations", "Removals/second")));
        tierStats.add(new TierMigrationStats(create(type, ACCUMULATOR.ADD, "Tier Migrations Operations", "Object moves/second")));
      }
      storeStats.add(new ReadStats(create(type, ACCUMULATOR.ADD, "Disk Read Operations", "Disk Reads/second")));
      storeStats.add(new WriteStats(create(type, ACCUMULATOR.ADD, "Disk Write Operations", "Disk Writes/second")));
      storeStats.add(new IOPSStats(create(type, ACCUMULATOR.ADD, "Disk IOPS", "Disk IO/second")));
      storeStats.add(new DeleteStats(create(type, ACCUMULATOR.ADD, "Removal Operations", "Removals/second")));
      storeStats.add(new ReadLatencyStats(create(type, ACCUMULATOR.ADD, "Read Latency", "ms")));
      storeStats.add(new WriteLatencyStats(create(type, ACCUMULATOR.ADD, "Write Latency", "ms")));
      storeStats.add(new BytesReadStats(create(type, ACCUMULATOR.ADD, "Bytes Read", "Bytes/second")));
      storeStats.add(new BytesWrittenStats(create(type, ACCUMULATOR.ADD, "Bytes Written", "Bytes/second")));
      storeStats.add(new TotalSizeStats(create(type, ACCUMULATOR.DIFF, "Total Size", "Bytes")));
      storeStats.add(new TotalEmptySpaceStats(create(type, ACCUMULATOR.DIFF, "Empty Space", "Bytes")));
      storeStats.add(new PartitionCountStats(create(type, ACCUMULATOR.DIFF, "Partition Count", "Partitions")));
    }
    else{
      future = null;
    }
  }

  public Map<String, Map<String, LinkedMovingAverageRecordDTO>> getStatistics(){
    Map<String, Map<String, LinkedMovingAverageRecordDTO>> response = new LinkedHashMap<>();
    if(!cacheStats.isEmpty()) {
      Map<String, LinkedMovingAverageRecordDTO> cache = new LinkedHashMap<>();
      response.put("cache", cache);
      for(CacheStats stats:cacheStats){
        Stats movingAverage = stats.movingAverage;
        if (movingAverage.supportMovingAverage()) {
          cache.put(movingAverage.getName(), ((LinkedMovingAverages)movingAverage).getRecord());
        }
      }
    }
    if(!tierStats.isEmpty()){
      Map<String, LinkedMovingAverageRecordDTO> tier = new LinkedHashMap<>();
      response.put("tier", tier);
      for(TierStats stats:tierStats){
        Stats movingAverage = stats.movingAverage;
        if (movingAverage.supportMovingAverage()) {
          tier.put(movingAverage.getName(), ((LinkedMovingAverages) movingAverage).getRecord());
        }
      }

    }
    if(!storeStats.isEmpty()){
      Map<String, LinkedMovingAverageRecordDTO> store = new LinkedHashMap<>();
      response.put("store", store);
      for(StorageStats stats:storeStats){
        Stats movingAverage = stats.movingAverage;
        if (movingAverage.supportMovingAverage()) {
          store.put(movingAverage.getName(), ((LinkedMovingAverages) movingAverage).getRecord());
        }
      }
    }
    return response;
  }

  @Override
  public void close()  {
    if(future != null) {
      future.cancel(true);
    }
    storeStats.clear();
  }

  @Override
  public void run() {
    io.mapsmessaging.storage.Statistics actualStats = resource.getStatistics();
    if (actualStats != null) {
      if (actualStats instanceof CacheStatistics) {
        CacheStatistics cacheStatistics = (CacheStatistics) actualStats;
        processCacheStatistics(cacheStatistics);
        actualStats = cacheStatistics.getStorageStatistics();
      }
      if (actualStats instanceof MemoryTierStatistics) {
        processTierStatistics((MemoryTierStatistics) actualStats);
        actualStats = ((MemoryTierStatistics) actualStats).getFileStatistics();
      }
      processStoreStatistics((StorageStatistics) actualStats);
    }
  }

  private void processCacheStatistics(CacheStatistics cacheStatistics) {
    for (CacheStats stats : cacheStats) {
      stats.update(cacheStatistics);
    }
  }

  private void processStoreStatistics(StorageStatistics storageStatistics) {
    for (StorageStats stats : storeStats) {
      stats.update(storageStatistics);
    }
  }

  private void processTierStatistics(MemoryTierStatistics memoryTierStatistics) {
    for (TierStats stats : tierStats) {
      stats.update(memoryTierStatistics);
    }
  }

  private abstract static class TierStats {

    private final Stats movingAverage;

    public TierStats(Stats movingAverage) {
      this.movingAverage = movingAverage;
    }

    protected void update(long value) {
      movingAverage.add(value);
    }

    public abstract void update(MemoryTierStatistics statistics);
  }

  public static class TierReadStats extends TierStats {

    public TierReadStats(Stats movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update(((StorageStatistics) statistics.getMemoryStatistics()).getReads());
    }
  }

  public static class TierWriteStats extends TierStats {

    public TierWriteStats(Stats movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update(((StorageStatistics) statistics.getMemoryStatistics()).getWrites());
    }
  }

  public static class TierDeleteStats extends TierStats {

    public TierDeleteStats(Stats movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update(((StorageStatistics) statistics.getMemoryStatistics()).getDeletes());
    }
  }

  public static class TierMigrationStats extends TierStats {

    public TierMigrationStats(Stats movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update(statistics.getMigratedCount());
    }
  }


  public static class TierSizeStats extends TierStats {

    public TierSizeStats(Stats movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update(((StorageStatistics) statistics.getMemoryStatistics()).getTotalSize());
    }
  }

  private abstract static class CacheStats {

    private final Stats movingAverage;

    public CacheStats(Stats movingAverage) {
      this.movingAverage = movingAverage;
    }

    protected void update(long value) {
      movingAverage.add(value);
    }

    public abstract void update(CacheStatistics statistics);

  }

  public static class CacheMissStats extends CacheStats {

    public CacheMissStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(CacheStatistics statistics) {
      super.update(statistics.getMiss());
    }
  }

  public static class CacheHitStats extends CacheStats {

    public CacheHitStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(CacheStatistics statistics) {
      super.update(statistics.getHit());
    }
  }

  public static class CacheSizeStats extends CacheStats {

    public CacheSizeStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(CacheStatistics statistics) {
      super.update(statistics.getSize());
    }
  }

  private abstract static class StorageStats {

    private final Stats movingAverage;

    public StorageStats(Stats movingAverage) {
      this.movingAverage = movingAverage;
    }

    protected void update(long value) {
      movingAverage.add(value);
    }

    public abstract void update(StorageStatistics statistics);

  }

  private static final class ReadStats extends StorageStats {

    public ReadStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getReads());
    }
  }

  private static final class WriteStats extends StorageStats {

    public WriteStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getWrites());
    }
  }

  private static final class IOPSStats extends StorageStats {

    public IOPSStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getIops());
    }
  }

  private static final class DeleteStats extends StorageStats {

    public DeleteStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getDeletes());
    }
  }

  private static final class TotalSizeStats extends StorageStats {

    public TotalSizeStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getTotalSize());
    }
  }

  private static final class TotalEmptySpaceStats extends StorageStats {

    public TotalEmptySpaceStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getTotalEmptySpace());
    }
  }

  private static final class PartitionCountStats extends StorageStats {

    public PartitionCountStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getPartitionCount());
    }
  }

  private static final class BytesWrittenStats extends StorageStats {

    public BytesWrittenStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getBytesWritten());
    }
  }

  private static final class BytesReadStats extends StorageStats {

    public BytesReadStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getBytesRead());
    }
  }

  private static final class ReadLatencyStats extends StorageStats {

    public ReadLatencyStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getReadLatency());
    }
  }

  private static final class WriteLatencyStats extends StorageStats {

    public WriteLatencyStats(Stats movingAverage) {
      super(movingAverage);
    }

    public void update(StorageStatistics statistics) {
      super.update(statistics.getWriteLatency());
    }
  }

}
