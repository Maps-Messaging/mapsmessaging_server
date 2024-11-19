/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.dto.rest.config.destination.ArchiveConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.CacheConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.S3ArchiveConfigDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.storage.AsyncStorage;
import io.mapsmessaging.storage.Statistics;
import io.mapsmessaging.storage.Storage;
import io.mapsmessaging.storage.StorageBuilder;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  public ResourceImpl() throws IOException {
    this(null, null, "Internal-Resource:" + INTERNAL_RESOURCE_COUNTER.incrementAndGet(), null);
  }

  @SneakyThrows
  public ResourceImpl(@Nullable MessageExpiryHandler messageExpiryHandler, @Nullable DestinationConfigDTO destinationConfig, @NotNull String fileName,
                      @Nullable ResourceProperties resourceProperties) throws IOException {
    keyGen = new AtomicLong(0);
    loaded = false;
    isClosed = false;
    name = fileName + "message.data";
    this.resourceProperties = resourceProperties;
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("basePath", fileName);
    StorageBuilder<Message> builder = new StorageBuilder<>();
    long idleTime = 0;
    String type = "Memory";
    Map<String, String> storeProperties = new LinkedHashMap<>();
    if (destinationConfig != null) {
      storeProperties.put("debug", ""+destinationConfig.isDebug());
      storeProperties.put("Sync", "" + destinationConfig.isSync());
      storeProperties.put("ItemCount", "" + destinationConfig.getItemCount());
      storeProperties.put("MaxPartitionSize", "" + destinationConfig.getMaxPartitionSize());
      storeProperties.put("ExpiredEventPoll", "" + destinationConfig.getExpiredEventPoll());
      idleTime = destinationConfig.getAutoPauseTimeout();
      type = destinationConfig.getType();
      if (type.equalsIgnoreCase("file")) {
        type = "Partition";
      }
      if (destinationConfig.getCache() != null) {
        CacheConfigDTO cacheConfig = destinationConfig.getCache();
        builder.setCache(cacheConfig.getType());
        builder.enableCacheWriteThrough(cacheConfig.isWriteThrough());
      }
      if(destinationConfig.getArchive() != null){
        ArchiveConfigDTO archiveConfig = destinationConfig.getArchive();
        properties.put("archiveName", archiveConfig.getName() );
        properties.put("archiveIdleTime", ""+archiveConfig.getIdleTime());
        properties.put("digestName", archiveConfig.getDigestAlgorithm());
        S3ArchiveConfigDTO s3ArchiveConfig = archiveConfig.getS3();
        if(s3ArchiveConfig != null){
          properties.put("S3AccessKeyId", s3ArchiveConfig.getAccessKeyId());
          properties.put("S3SecretAccessKey", s3ArchiveConfig.getSecretAccessKey());
          properties.put("S3RegionName", s3ArchiveConfig.getRegionName());
          properties.put("S3BucketName", s3ArchiveConfig.getBucketName());
          properties.put("S3CompressEnabled", ""+s3ArchiveConfig.isCompression());
        }
      }
    }
    builder.setProperties(properties)
        .setName(name)
        .setFactory(new MessageFactory())
        .setProperties(storeProperties)
        .setStorageType(type);
    if (messageExpiryHandler != null) {
      builder.setExpiredHandler(messageExpiryHandler);
    }

    Storage<Message> s = builder.build();
    persistent = !(type.equalsIgnoreCase("Memory"));
    store = new AsyncStorage<>(s);
    if (idleTime > 0) {
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
