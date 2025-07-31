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

import io.mapsmessaging.selector.ml.ModelStore;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachingModelStore implements ModelStore {

  private final ModelStore store;
  private final Map<String, byte[]> cache;

  public CachingModelStore(ModelStore store) {
    this.store = store;
    cache = new ConcurrentHashMap<>();
  }

  public void stop(){

  }

  @Override
  public void saveModel(String s, byte[] bytes) throws IOException {
    cache.put(s, bytes);
    store.saveModel(s, bytes);
  }

  @Override
  public byte[] loadModel(String s) throws IOException {
    if(cache.containsKey(s)){
      return cache.get(s);
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
}
