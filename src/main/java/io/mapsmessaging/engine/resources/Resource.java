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
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.storage.Storage;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public abstract class Resource implements BaseResource {

  private static final LongAdder totalRetained = new LongAdder();

  public static long getTotalRetained(){
    return totalRetained.sum();
  }

  private final String name;
  private final String mappedName;
  protected boolean isClosed;
  private final AtomicLong keyGen;
  private long retainedIdentifier;
  private Storage<Message> store;
  protected boolean persistent;


  protected Resource(String name, String mappedName) {
    this.name = name;
    this.mappedName = mappedName;
    keyGen = new AtomicLong(0);
    isClosed = false;
    retainedIdentifier = -1;
  }

  protected void setStore(Storage<Message> store){
    this.store = store;
  }

  @Override
  public void close() throws IOException {
    isClosed = true;
    store.close();
  }

  public boolean isPersistent(){
    return persistent;
  }

  public String getName() {
    return name;
  }

  public String getMappedName() {
    return mappedName;
  }

  public long getRetainedIdentifier() {
    return retainedIdentifier;
  }

  @Override
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

  @Override
  public void remove(long key) throws IOException {
    ThreadLocalContext.checkDomain(DestinationImpl.RESOURCE_TASK_KEY);
    if (key == retainedIdentifier) {
      totalRetained.decrement();
      retainedIdentifier = -1;
    }
    store.remove(key);
  }

  @Override
  public boolean isEmpty(){
    return store.isEmpty();
  }

  @Override
  public synchronized void delete() throws IOException {
    store.delete();
  }

  @Override
  public synchronized Message get(long key) throws IOException {
    Message message = store.get(key);
    if(message == null){
      System.err.println("Unable to locate "+key);
    }
    return message;
  }

  @Override
  public synchronized long size() throws IOException {
    return store.size();
  }
}
