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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MemoryResource extends Resource {

  private final Map<Long, Message> store;

  public MemoryResource(String name) {
    super(name, name);
    store = new LinkedHashMap<>();
  }

  @Override
  public void delete() throws IOException {
    if (!isClosed) {
      close();
    }
    store.clear();
  }

  @Override
  public synchronized void add(Message message) throws IOException {
    checkIsClosed();
    super.add(message);
    store.put(message.getIdentifier(), message);
  }

  @Override
  public synchronized Message get(long key) throws IOException {
    checkIsClosed();
    return store.get(key);
  }

  @Override
  public synchronized void remove(long key) throws IOException {
    checkIsClosed();
    store.remove(key);
    super.remove(key);
  }

  @Override
  public synchronized long size() throws IOException {
    checkIsClosed();
    return store.size();
  }

  @Override
  public boolean isEmpty() {
    return store.isEmpty();
  }

  @Override
  public synchronized void close() throws IOException {
    super.close();
    store.clear();
  }
}
