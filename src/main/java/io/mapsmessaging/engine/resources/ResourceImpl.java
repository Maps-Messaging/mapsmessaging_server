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

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.config.destination.DestinationConfig;
import io.mapsmessaging.dto.rest.config.destination.*;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.storage.*;
import io.mapsmessaging.storage.impl.file.config.DeferredConfig;
import io.mapsmessaging.storage.impl.file.config.PartitionStorageConfig;
import io.mapsmessaging.storage.impl.file.config.S3Config;
import io.mapsmessaging.storage.impl.memory.MemoryStorageConfig;
import io.mapsmessaging.storage.impl.tier.memory.MemoryTierConfig;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ResourceImpl implements Resource {

  private static final AtomicLong INTERNAL_RESOURCE_COUNTER = new AtomicLong(0);

  @Getter
  private final String name;

  @Getter
  private final boolean persistent;

  private final AtomicLong keyGen;
  private volatile boolean loaded;
  private final AsyncStorage<Message> store;

  private boolean isClosed;

  @Getter
  private  final ResourceProperties resourceProperties;

  public ResourceImpl(){
    this(null, null, "Internal-Resource:" + INTERNAL_RESOURCE_COUNTER.incrementAndGet(), null);
  }

  @SneakyThrows
  public ResourceImpl(@Nullable MessageExpiryHandler messageExpiryHandler, @Nullable DestinationConfigDTO destinationConfig, @NotNull String fileName,
                      @Nullable ResourceProperties resourceProperties) {
    keyGen = new AtomicLong(0);
    loaded = false;
    isClosed = false;
    name = fileName + "message.data";
    this.resourceProperties = resourceProperties;

    if (destinationConfig == null) {
      destinationConfig = new DestinationConfig();
      destinationConfig.setType("Memory");
      MemoryStorageConfigDTO memoryStorageConfig = new MemoryStorageConfigDTO();
      memoryStorageConfig.setCapacity(2);
      memoryStorageConfig.setExpiredEventPoll(-1);
      destinationConfig.setStorageConfig(memoryStorageConfig);
    }

    // Convert to storage configs
    StorageConfigDTO config = destinationConfig.getStorageConfig();
    StorageConfig storageConfig = convert(config);
    if (storageConfig == null) {
      throw new IOException("Cannot build config");
    }

    StorageBuilder<Message> builder = new StorageBuilder<>();
    builder.setConfig(storageConfig)
        .setName(name)
        .setFactory(new MessageFactory());

    if (config instanceof PartitionStorageConfigDTO partitionStorageConfig) {
      partitionStorageConfig.setFileName(fileName);
    } else if (config instanceof MemoryTierConfigDTO memoryConfig) {
      memoryConfig.getPartitionStorageConfig().setFileName(fileName);
    }

    if (destinationConfig.getCache() != null) {
      CacheConfigDTO cacheConfig = destinationConfig.getCache();
      builder.setCache(cacheConfig.getType())
          .enableCacheWriteThrough(cacheConfig.isWriteThrough());
    }

    if (messageExpiryHandler != null) {
      builder.setExpiredHandler(messageExpiryHandler);
    }

    Storage<Message> s = builder.build();
    persistent = config instanceof MemoryStorageConfigDTO;
    store = new AsyncStorage<>(s);
    if (destinationConfig.getAutoPauseTimeout() > 0) {
      store.enableAutoPause(TimeUnit.SECONDS.toMillis(destinationConfig.getAutoPauseTimeout()));  // Convert to milliseconds
    }
  }

  @Override
  public void close() throws IOException {
    if (!isClosed) {
      isClosed = true;
      store.close();
    }
  }

  private StorageConfig convert(StorageConfigDTO config) {
    if(config instanceof MemoryStorageConfigDTO memoryConfigDTO) {
      MemoryStorageConfig memoryStorageConfig = new MemoryStorageConfig();
      memoryStorageConfig.setCapacity(memoryConfigDTO.getCapacity());
      memoryStorageConfig.setExpiredEventPoll(memoryConfigDTO.getExpiredEventPoll());
      memoryStorageConfig.setType(memoryConfigDTO.getType());
      memoryStorageConfig.setDebug(config.isDebug());
      memoryStorageConfig.setType("Memory");
      return memoryStorageConfig;

    }
    else if(config instanceof PartitionStorageConfigDTO partitionStorageConfigDTO) {
      PartitionStorageConfig partitionStorageConfig = new PartitionStorageConfig();
      partitionStorageConfig.setType(partitionStorageConfigDTO.getType());
      partitionStorageConfig.setDebug(config.isDebug());
      partitionStorageConfig.setCapacity(partitionStorageConfigDTO.getCapacity());
      partitionStorageConfig.setExpiredEventPoll(partitionStorageConfigDTO.getExpiredEventPoll());
      partitionStorageConfig.setMaxPartitionSize(partitionStorageConfigDTO.getMaxPartitionSize());
      partitionStorageConfig.setItemCount(partitionStorageConfigDTO.getItemCount());
      partitionStorageConfig.setDeferredConfig(convert(partitionStorageConfigDTO.getDeferredConfig()));
      partitionStorageConfig.setSync(partitionStorageConfigDTO.isSync());
      partitionStorageConfig.setType("Partition");
      return partitionStorageConfig;
    }
    else if(config instanceof MemoryTierConfigDTO memoryTierConfigDTO) {
      MemoryTierConfig memoryTierConfig = new MemoryTierConfig();
      memoryTierConfig.setDebug(config.isDebug());
      memoryTierConfig.setType(memoryTierConfigDTO.getType());
      memoryTierConfig.setMaximumCount(memoryTierConfigDTO.getMaximumCount());
      memoryTierConfig.setMigrationTime(memoryTierConfigDTO.getMigrationTime());
      memoryTierConfig.setScanInterval(memoryTierConfigDTO.getScanInterval());
      memoryTierConfig.setMemoryStorageConfig((MemoryStorageConfig) convert(memoryTierConfigDTO.getMemoryStorageConfig()));
      memoryTierConfig.setPartitionStorageConfig((PartitionStorageConfig) convert(memoryTierConfigDTO.getPartitionStorageConfig()));
      memoryTierConfig.setType("MemoryTier");
      return memoryTierConfig;
    }
    return null;
  }


  private DeferredConfig convert(DeferredConfigDTO config) {
    DeferredConfig deferredConfig = new DeferredConfig();
    deferredConfig.setDeferredName(config.getDeferredName());
    deferredConfig.setDigestName(config.getDigestName());
    deferredConfig.setIdleTime(config.getIdleTime());
    deferredConfig.setMigrationDestination(config.getMigrationDestination());
    if(config.getS3Config() != null) {
      deferredConfig.setS3Config(convert(config.getS3Config()));
    }
    return deferredConfig;
  }

  private S3Config convert(io.mapsmessaging.dto.rest.config.S3Config config) {
    S3Config s3Config = new S3Config();
    s3Config.setBucketName(config.getBucket());
    s3Config.setRegionName(config.getRegion());
    s3Config.setAccessKeyId(config.getAccessKey());
    s3Config.setSecretAccessKey(config.getSecretKey());
    s3Config.setCompression(config.isCompression());
    return s3Config;
  }

  @Override
  public void add(Message message) throws IOException {
    ThreadLocalContext.checkDomain(DestinationImpl.RESOURCE_TASK_KEY);
    message.setIdentifier(getNextIdentifier());
    getFromFuture(store.add(message));
  }

  public void checkLoaded() {
    if (!loaded) {
      if (persistent) {
        try {
          keyGen.set(store.getLastKey().get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException | IOException e) {
          //ignore
        }

      }
      loaded = true;
    }
  }

  @Override
  public void keepOnly(List<Long> validKeys) throws IOException {
    checkLoaded();
    store.keepOnly(validKeys);
  }

  @Override
  public long getNextIdentifier() {
    checkLoaded();
    return keyGen.incrementAndGet();
  }

  @Override
  public void remove(long key) throws IOException {
    ThreadLocalContext.checkDomain(DestinationImpl.RESOURCE_TASK_KEY);
    getFromFuture(store.remove(key));
  }

  @Override
  public void delete() throws IOException {
    store.delete();
  }

  @Override
  public boolean isEmpty() {
    return getFromFuture(store.isEmpty());
  }

  @Override
  public Message get(long key) throws IOException {
    return getFromFuture(store.get(key));
  }

  @Override
  public boolean contains(Long id) throws IOException {
    return getFromFuture(store.contains(id));
  }

  @Override
  public List<Long> getKeys() throws IOException{
    return getFromFuture(store.getKeys());
  }

  @Override
  public long size() throws IOException {
    return getFromFuture(store.size());
  }

  @Override
  public @Nullable Statistics getStatistics() {
    return getFromFuture(store.getStatistics());
  }
}
