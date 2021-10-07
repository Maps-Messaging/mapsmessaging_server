package io.mapsmessaging.engine.resources;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.stats.Statistics;
import io.mapsmessaging.storage.Storage;
import io.mapsmessaging.storage.StorageStatistics;
import io.mapsmessaging.storage.impl.cache.CacheStatistics;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ResourceStatistics extends Statistics implements AutoCloseable, Runnable {

  private final Future<?> future;
  private final Storage<Message> storage;
  private final List<StorageStats> storeStats;
  private final List<CacheStats> cacheStats;

  public ResourceStatistics(Resource resource){
    storeStats = new ArrayList<>();
    cacheStats = new ArrayList<>();
    storage = resource.getStore();
    future = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
    storeStats.add(new ReadStats(create(ACCUMULATOR.ADD,"Disk Read Operations", "Disk Reads/second" )));
    storeStats.add(new WriteStats(create(ACCUMULATOR.ADD,"Disk Write Operations", "Disk Writes/second" )));
    storeStats.add(new IOPSStats(create(ACCUMULATOR.ADD,"Disk IOPS", "Disk IO/second" )));
    storeStats.add(new DeleteStats(create(ACCUMULATOR.ADD,"Removal Operations", "Removals/second" )));
    storeStats.add(new ReadLatencyStats(create(ACCUMULATOR.ADD,"Read Latency", "ms" )));
    storeStats.add(new WriteLatencyStats(create(ACCUMULATOR.ADD,"Write Latency", "ms" )));
    storeStats.add(new BytesReadStats(create(ACCUMULATOR.ADD,"Bytes Read", "Bytes/second" )));
    storeStats.add(new BytesWrittenStats(create(ACCUMULATOR.ADD,"Bytes Written", "Bytes/second" )));
    storeStats.add(new TotalSizeStats(create(ACCUMULATOR.ADD,"Total Size", "Bytes" )));
    storeStats.add(new TotalEmptySpaceStats(create(ACCUMULATOR.ADD,"Empty Space", "Bytes" )));
    storeStats.add(new PartitionCountStats(create(ACCUMULATOR.ADD,"Partition Count", "Partitions" )));
    if(resource.getStore().getStatistics() instanceof CacheStatistics){
      cacheStats.add(new CacheHitStats(create(ACCUMULATOR.ADD,"Cache Hits", "Hits/second" )));
      cacheStats.add(new CacheMissStats(create(ACCUMULATOR.ADD,"Cache Miss", "Hits/second" )));
      cacheStats.add(new CacheSizeStats(create(ACCUMULATOR.ADD,"Cache Size", "Entries" )));
    }

  }

  @Override
  public void close() throws Exception {
    future.cancel(true);
    storeStats.clear();
  }

  @Override
  public void run() {
    io.mapsmessaging.storage.Statistics storeStats = storage.getStatistics();
    if(storeStats instanceof CacheStatistics){
      CacheStatistics cacheStatistics = (CacheStatistics)storeStats;
      processCacheStatistics(cacheStatistics);
      processStoreStatistics((StorageStatistics) cacheStatistics.getStorageStatistics());
    }
    else{
      processStoreStatistics((StorageStatistics) storeStats);
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

  private abstract class CacheStats{
    private final LinkedMovingAverages movingAverage;

    public CacheStats(LinkedMovingAverages movingAverage){
      this.movingAverage = movingAverage;
    }

    protected void update(long value){
      movingAverage.add(value);
    }

    public abstract void update(CacheStatistics statistics);

  }

  public class CacheMissStats extends CacheStats{

    public CacheMissStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(CacheStatistics statistics){
      super.update(statistics.getMiss());
    }
  }

  public class CacheHitStats extends CacheStats{

    public CacheHitStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(CacheStatistics statistics){
      super.update(statistics.getHit());
    }
  }

  public class CacheSizeStats extends CacheStats{

    public CacheSizeStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(CacheStatistics statistics){
      super.update(statistics.getSize());
    }
  }

  private abstract class StorageStats{
    private final LinkedMovingAverages movingAverage;

    public StorageStats(LinkedMovingAverages movingAverage){
      this.movingAverage = movingAverage;
    }

    protected void update(long value){
      movingAverage.add(value);
    }

    public abstract void update(StorageStatistics statistics);

  }

  private final class ReadStats extends StorageStats {

    public ReadStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getReads());
    }
  }

  private final class WriteStats extends StorageStats {

    public WriteStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getWrites());
    }
  }

  private final class IOPSStats extends StorageStats {

    public IOPSStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getIops());
    }
  }

  private final class DeleteStats extends StorageStats {

    public DeleteStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getDeletes());
    }
  }

  private final class TotalSizeStats extends StorageStats {

    public TotalSizeStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getTotalSize());
    }
  }

  private final class TotalEmptySpaceStats extends StorageStats {

    public TotalEmptySpaceStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getTotalEmptySpace());
    }
  }

  private final class PartitionCountStats extends StorageStats {

    public PartitionCountStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getPartitionCount());
    }
  }

  private final class BytesWrittenStats extends StorageStats {

    public BytesWrittenStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getBytesWritten());
    }
  }

  private final class BytesReadStats extends StorageStats {

    public BytesReadStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getBytesRead());
    }
  }

  private final class ReadLatencyStats extends StorageStats {

    public ReadLatencyStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getReadLatency());
    }
  }
  private final class WriteLatencyStats extends StorageStats {

    public WriteLatencyStats(LinkedMovingAverages movingAverage){
      super(movingAverage);
    }

    public void update(StorageStatistics statistics){
      super.update(statistics.getWriteLatency());
    }
  }

}
