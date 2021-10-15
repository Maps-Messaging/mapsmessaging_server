package io.mapsmessaging.engine.resources;

import io.mapsmessaging.engine.stats.Statistics;
import io.mapsmessaging.storage.StorageStatistics;
import io.mapsmessaging.storage.impl.cache.CacheStatistics;
import io.mapsmessaging.storage.impl.tier.memory.MemoryTierStatistics;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ResourceStatistics extends Statistics implements AutoCloseable, Runnable {

  private final Future<?> future;
  private final Resource resource;
  private final List<StorageStats> storeStats;
  private final List<CacheStats> cacheStats;
  private final List<TierStats> tierStats;

  public ResourceStatistics(Resource resource){
    storeStats = new ArrayList<>();
    cacheStats = new ArrayList<>();
    tierStats = new ArrayList<>();
    this.resource = resource;
    future = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);


    io.mapsmessaging.storage.Statistics statistics = resource.getStatistics();
    if(statistics instanceof CacheStatistics){
      cacheStats.add(new CacheHitStats(create(ACCUMULATOR.DIFF, "Cache Hits", "Hits/second")));
      cacheStats.add(new CacheMissStats(create(ACCUMULATOR.DIFF, "Cache Miss", "Hits/second")));
      cacheStats.add(new CacheSizeStats(create(ACCUMULATOR.DIFF, "Cache Size", "Entries")));
      statistics = ((CacheStatistics)statistics).getStorageStatistics();
    }

    if(statistics instanceof MemoryTierStatistics){
      tierStats.add(new TierSizeStats(create(ACCUMULATOR.ADD, "Top Tier Entries", "Entries")));
      tierStats.add(new TierReadStats(create(ACCUMULATOR.ADD, "Top Tier Read Operations", "Reads/second")));
      tierStats.add(new TierWriteStats(create(ACCUMULATOR.ADD,"Top Tier Write Operations", "Writes/second" )));
      tierStats.add(new TierDeleteStats(create(ACCUMULATOR.ADD,"Top Tier Delete Operations", "Removals/second" )));
      tierStats.add(new TierMigrationStats(create(ACCUMULATOR.ADD, "Tier Migrations Operations", "Object moves/second")));
    }
    storeStats.add(new ReadStats(create(ACCUMULATOR.ADD, "Disk Read Operations", "Disk Reads/second")));
    storeStats.add(new WriteStats(create(ACCUMULATOR.ADD,"Disk Write Operations", "Disk Writes/second" )));
    storeStats.add(new IOPSStats(create(ACCUMULATOR.ADD,"Disk IOPS", "Disk IO/second" )));
    storeStats.add(new DeleteStats(create(ACCUMULATOR.ADD,"Removal Operations", "Removals/second" )));
    storeStats.add(new ReadLatencyStats(create(ACCUMULATOR.ADD,"Read Latency", "ms" )));
    storeStats.add(new WriteLatencyStats(create(ACCUMULATOR.ADD,"Write Latency", "ms" )));
    storeStats.add(new BytesReadStats(create(ACCUMULATOR.ADD,"Bytes Read", "Bytes/second" )));
    storeStats.add(new BytesWrittenStats(create(ACCUMULATOR.ADD,"Bytes Written", "Bytes/second" )));
    storeStats.add(new TotalSizeStats(create(ACCUMULATOR.DIFF,"Total Size", "Bytes" )));
    storeStats.add(new TotalEmptySpaceStats(create(ACCUMULATOR.DIFF,"Empty Space", "Bytes" )));
    storeStats.add(new PartitionCountStats(create(ACCUMULATOR.DIFF,"Partition Count", "Partitions" )));

  }

  @Override
  public void close() throws Exception {
    future.cancel(true);
    storeStats.clear();
  }

  @Override
  public void run() {
    io.mapsmessaging.storage.Statistics actualStats = resource.getStatistics();
    if(actualStats != null) {
      if (actualStats instanceof CacheStatistics) {
        CacheStatistics cacheStatistics = (CacheStatistics) actualStats;
        processCacheStatistics(cacheStatistics);
        actualStats = cacheStatistics.getStorageStatistics();
      }
      if(actualStats instanceof MemoryTierStatistics){
        processTierStatistics((MemoryTierStatistics) actualStats);
        actualStats = ((MemoryTierStatistics)actualStats).getFileStatistics();
      }
      processStoreStatistics((StorageStatistics) actualStats);
    }
  }

  private void processCacheStatistics(CacheStatistics cacheStatistics){
    for(CacheStats stats:cacheStats){
      stats.update(cacheStatistics);
    }
  }

  private void processStoreStatistics(StorageStatistics storageStatistics){
    for(StorageStats stats:storeStats){
      stats.update(storageStatistics);
    }
  }

  private void processTierStatistics(MemoryTierStatistics memoryTierStatistics){
    for(TierStats stats:tierStats){
      stats.update(memoryTierStatistics);
    }
  }

  private abstract static class TierStats{
    private final LinkedMovingAverages movingAverage;

    public TierStats(LinkedMovingAverages movingAverage){
      this.movingAverage = movingAverage;
    }

    protected void update(long value){
      movingAverage.add(value);
    }

    public abstract void update(MemoryTierStatistics statistics);
  }

  public static class TierReadStats extends TierStats{

    public TierReadStats(LinkedMovingAverages movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update( ((StorageStatistics) statistics.getMemoryStatistics()).getReads());
    }
  }

  public static class TierWriteStats extends TierStats{

    public TierWriteStats(LinkedMovingAverages movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update( ((StorageStatistics) statistics.getMemoryStatistics()).getWrites());
    }
  }

  public static class TierDeleteStats extends TierStats{

    public TierDeleteStats(LinkedMovingAverages movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update( ((StorageStatistics) statistics.getMemoryStatistics()).getDeletes());
    }
  }

  public static class TierMigrationStats extends TierStats{

    public TierMigrationStats(LinkedMovingAverages movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update(statistics.getMigratedCount());
    }
  }


  public static class TierSizeStats extends TierStats{

    public TierSizeStats(LinkedMovingAverages movingAverage) {
      super(movingAverage);
    }

    @Override
    public void update(MemoryTierStatistics statistics) {
      super.update( ((StorageStatistics) statistics.getMemoryStatistics()).getTotalSize());
    }
  }

  private abstract static class CacheStats{
    private final LinkedMovingAverages movingAverage;

    public CacheStats(LinkedMovingAverages movingAverage){
      this.movingAverage = movingAverage;
    }

    protected void update(long value){
      movingAverage.add(value);
    }

    public abstract void update(CacheStatistics statistics);

  }

  public static class CacheMissStats extends CacheStats{

    public CacheMissStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(CacheStatistics statistics){
      super.update(statistics.getMiss());
    }
  }

  public static class CacheHitStats extends CacheStats{

    public CacheHitStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(CacheStatistics statistics){
      super.update(statistics.getHit());
    }
  }

  public static class CacheSizeStats extends CacheStats{

    public CacheSizeStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(CacheStatistics statistics){
      super.update(statistics.getSize());
    }
  }

  private abstract static class StorageStats{
    private final LinkedMovingAverages movingAverage;

    public StorageStats(LinkedMovingAverages movingAverage){
      this.movingAverage = movingAverage;
    }

    protected void update(long value){
      movingAverage.add(value);
    }

    public abstract void update(StorageStatistics statistics);

  }

  private static final class ReadStats extends StorageStats {

    public ReadStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getReads());
    }
  }

  private static final class WriteStats extends StorageStats {

    public WriteStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getWrites());
    }
  }

  private static final class IOPSStats extends StorageStats {

    public IOPSStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getIops());
    }
  }

  private static final class DeleteStats extends StorageStats {

    public DeleteStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getDeletes());
    }
  }

  private static final class TotalSizeStats extends StorageStats {

    public TotalSizeStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getTotalSize());
    }
  }

  private static final class TotalEmptySpaceStats extends StorageStats {

    public TotalEmptySpaceStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getTotalEmptySpace());
    }
  }

  private static final class PartitionCountStats extends StorageStats {

    public PartitionCountStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getPartitionCount());
    }
  }

  private static final class BytesWrittenStats extends StorageStats {

    public BytesWrittenStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getBytesWritten());
    }
  }

  private static final class BytesReadStats extends StorageStats {

    public BytesReadStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getBytesRead());
    }
  }

  private static final class ReadLatencyStats extends StorageStats {

    public ReadLatencyStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getReadLatency());
    }
  }
  private static final class WriteLatencyStats extends StorageStats {

    public WriteLatencyStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getWriteLatency());
    }
  }

}
