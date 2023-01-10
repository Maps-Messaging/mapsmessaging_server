/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationPathManager;
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
  public ResourceImpl(@Nullable MessageExpiryHandler messageExpiryHandler, @Nullable DestinationPathManager pathManager, @NotNull String fileName,
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
    if (pathManager != null) {
      storeProperties.put("Sync", "" + pathManager.isEnableSync());
      storeProperties.put("ItemCount", "" + pathManager.getItemCount());
      storeProperties.put("MaxPartitionSize", "" + pathManager.getPartitionSize());
      storeProperties.put("ExpiredEventPoll", "" + pathManager.getExpiredEventPoll());
      idleTime = pathManager.getIdleTime();
      type = pathManager.getType();
      if (type.equalsIgnoreCase("file")) {
        type = "Partition";
      }
      if (pathManager.isEnableCache()) {
        builder.setCache(pathManager.getCacheType());
        builder.enableCacheWriteThrough(pathManager.isWriteThrough());
      }
      if(pathManager.getArchiveName() != null){
        properties.put("archiveName", pathManager.getArchiveName() );
        properties.put("archiveIdleTime", ""+pathManager.getArchiveIdleTime());
        properties.put("digestName", pathManager.getDigestAlgorithm());
        properties.put("S3AccessKeyId", pathManager.getS3AccessKeyId());
        properties.put("S3SecretAccessKey", pathManager.getS3SecretAccessKey());
        properties.put("S3RegionName", pathManager.getS3RegionName());
        properties.put("S3BucketName", pathManager.getS3BucketName());
        properties.put("S3CompressEnabled", ""+pathManager.isS3Compression());
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
      store.enableAutoPause(TimeUnit.SECONDS.toMillis(pathManager.getIdleTime()));  // Convert to milliseconds
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
