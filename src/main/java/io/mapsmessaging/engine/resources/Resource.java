/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
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
import io.mapsmessaging.storage.Storage;
import io.mapsmessaging.storage.StorageBuilder;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Resource implements AutoCloseable {

  private static final LongAdder totalRetained = new LongAdder();

  public static long getTotalRetained(){
    return totalRetained.sum();
  }

  private static final AtomicLong INTERNAL_RESOURCE_COUNTER = new AtomicLong(0);

  private final @Getter String name;
  private final @Getter Storage<Message> store;
  private final @Getter boolean persistent;
  private final AtomicLong keyGen;

  private @Getter long retainedIdentifier;

  private boolean isClosed;

  public Resource() throws IOException {
    this(null, null, "Internal-Resource:"+INTERNAL_RESOURCE_COUNTER.incrementAndGet());
  }

  public Resource(@Nullable MessageExpiryHandler messageExpiryHandler, @Nullable DestinationPathManager pathManager, @NotNull String fileName) throws IOException {
    keyGen = new AtomicLong(0);
    isClosed = false;
    retainedIdentifier = -1;
    name = fileName+"message.data";
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("basePath", fileName);
    StorageBuilder<Message> builder = new StorageBuilder<>();

    String type = "Memory";
    Map<String, String> storeProperties = new LinkedHashMap<>();
    if(pathManager != null) {
      storeProperties.put("Sync", "" + pathManager.isEnableSync());
      storeProperties.put("ItemCount", ""+pathManager.getItemCount());
      storeProperties.put("MaxPartitionSize",""+ pathManager.getPartitionSize());
      storeProperties.put("ExpiredEventPoll", ""+pathManager.getExpiredEventPoll());
      type = pathManager.getType();
      if(type.equalsIgnoreCase("file")){
        type = "Partition";
      }
      if(pathManager.isEnableCache()){
        builder.setCache(pathManager.getCacheType());
        builder.enableCacheWriteThrough(pathManager.isWriteThrough());
      }
    }
    builder.setProperties(properties)
        .setName(name)
        .setFactory(new MessageFactory())
        .setProperties(storeProperties)
        .setStorageType(type);
    if(messageExpiryHandler != null){
      builder.setExpiredHandler(messageExpiryHandler);
    }

    store = builder.build();
    persistent = !(type.equalsIgnoreCase("Memory"));
  }

  @Override
  public void close() throws IOException {
    if(!isClosed) {
      isClosed = true;
      store.close();
    }
  }

  public void add(Message message) throws IOException {
    ThreadLocalContext.checkDomain(DestinationImpl.RESOURCE_TASK_KEY);
    message.setIdentifier(getNextIdentifier());
    if (message.isRetain()) {
      if (message.getOpaqueData() == null || message.getOpaqueData().length == 0) {
        retainedIdentifier = -1;
        totalRetained.decrement();
      } else {
        retainedIdentifier = message.getIdentifier();
        totalRetained.increment();
      }
    }
    store.add(message);
  }

  protected long getNextIdentifier() {
    return keyGen.incrementAndGet();
  }

  public void remove(long key) throws IOException {
    ThreadLocalContext.checkDomain(DestinationImpl.RESOURCE_TASK_KEY);
    if (key == retainedIdentifier) {
      totalRetained.decrement();
      retainedIdentifier = -1;
    }
    store.remove(key);
  }

  public boolean isEmpty(){
    return store.isEmpty();
  }

  public synchronized void delete() throws IOException {
    store.delete();
  }

  public synchronized Message get(long key) throws IOException {
    return store.get(key);
  }

  public synchronized long size() throws IOException {
    return store.size();
  }
}
