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

package io.mapsmessaging.ml;

import io.mapsmessaging.config.ml.MLModelManagerConfig;
import io.mapsmessaging.dto.rest.config.ml.AutoRefreshConfig;
import io.mapsmessaging.selector.ml.ModelStore;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;


import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CachingModelStore implements ModelStore {

  private final ModelStore store;
  private final Map<String, CacheEntry> cache;
  private final int cacheTime;
  private final int cacheSize;
  private final ScheduledFuture<?> trimTask;
  private final ScheduledFuture<?> autofetchTask;

  public CachingModelStore(ModelStore store, MLModelManagerConfig modelStoreConfig) {
    this.store = store;
    this.cacheTime = (modelStoreConfig.getCacheExpiryMinutes() * 60 *1000);
    this.cacheSize = modelStoreConfig.getCacheSize();

    cache = new ConcurrentHashMap<>();
    if(cacheTime > 0 || cacheSize > 0) {
      trimTask = SimpleTaskScheduler.getInstance().schedule(this::trimCache, 1, TimeUnit.MINUTES);
    }
    else{
      trimTask = null;
    }
    AutoRefreshConfig autoRefreshConfig = modelStoreConfig.getAutoRefresh();
    if(autoRefreshConfig != null && autoRefreshConfig.isEnabled()) {
      autofetchTask = SimpleTaskScheduler.getInstance().schedule(this::processAutoRefresh, autoRefreshConfig.getIntervalMinutes(), TimeUnit.MINUTES);
    }
    else{
      autofetchTask = null;
    }
    for(String name: modelStoreConfig.getPreloadModels()){
      try {
        loadModel(name);
      } catch (IOException e) {
        // log this
      }
    }
  }

  public void stop(){
    if(trimTask != null){
      trimTask.cancel(true);
    }
    if(autofetchTask != null){
      autofetchTask.cancel(true);
    }
  }

  @Override
  public void saveModel(String s, byte[] bytes) throws IOException {
    cache.put(s, new CacheEntry(s, bytes));
    store.saveModel(s, bytes);
  }

  @Override
  public byte[] loadModel(String s) throws IOException {
    if(cache.containsKey(s)){
      return cache.get(s).getData();
    }
    return store.loadModel(s);
  }

  @Override
  public boolean modelExists(String s) throws IOException {
    if(cache.containsKey(s)){
      return true;
    }
    return store.modelExists(s);
  }

  @Override
  public boolean deleteModel(String s) throws IOException {
    cache.remove(s);
    return store.deleteModel(s);
  }

  private void processAutoRefresh(){
    for(Map.Entry<String, CacheEntry> entry : cache.entrySet()){
      try {
        byte[] reloadedData = store.loadModel(entry.getKey());
        if(reloadedData != null){
          entry.getValue().setData(reloadedData);
        }
      } catch (IOException e) {
        // Log this
      }
    }
  }

  private void trimCache(){
    List<CacheEntry> sortedEntries = cache.values().stream()
        .sorted(Comparator.comparingLong(CacheEntry::getAccessTime))
        .toList();

    long cutoff = System.currentTimeMillis() - cacheTime;

    // Remove expired
    for (CacheEntry entry : sortedEntries) {
      if (entry.getAccessTime() < cutoff) {
        cache.remove(entry.getKey());
      }
    }

    // Enforce size limit
    List<CacheEntry> remaining = cache.values().stream()
        .sorted(Comparator.comparingLong(CacheEntry::getAccessTime))
        .toList();

    int index = 0;
    while (cache.size() > cacheSize && index < remaining.size()) {
      cache.remove(remaining.get(index++).getKey());
    }
  }


  public static final class CacheEntry{
    private byte[] data;
    @Getter
    private final String key;
    @Getter
    private long accessTime;
    public CacheEntry(String key, byte[] data){
      this.key = key;
      this.data = data;
      this.accessTime = System.currentTimeMillis();
    }

    public byte[] getData(){
      accessTime = System.currentTimeMillis();
      return data;
    }

    public void setData(byte[] data){
      accessTime = System.currentTimeMillis();
      this.data = data;
    }

  }
}
