/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.engine.resources;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.utilities.threads.tasks.ThreadLocalContext;

public abstract class Resource implements Closeable {

  private static final LongAdder totalRetained = new LongAdder();
  public static long getTotalRetained(){
    return totalRetained.sum();
  }

  private final String name;
  private final String mappedName;
  protected boolean isClosed;
  private final AtomicLong keyGen;
  private long retainedIdentifier;

  public Resource(String name, String mappedName) {
    this.name = name;
    this.mappedName = mappedName;
    keyGen = new AtomicLong(0);
    isClosed = false;
    retainedIdentifier = -1;
  }

  public abstract boolean isEmpty();

  public abstract Iterator<Long> getIterator();

  @Override
  public void close() {
    isClosed = true;
  }

  protected void checkIsClosed() throws IOException {
    if (isClosed) {
      throw new IOException("Resource:" + name + " is closed");
    }
  }

  protected long getNextIdentifier() {
    return keyGen.incrementAndGet();
  }

  public String getName() {
    return name;
  }

  public String getMappedName() {
    return mappedName;
  }

  public abstract void delete() throws IOException;

  // While this function doesn't throw an exception, classes that extend it do
  @java.lang.SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
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
  }

  public abstract Message get(long key) throws IOException;

  // While this function doesn't throw an exception, classes that extend it do
  @java.lang.SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
  public void remove(long key) throws IOException {
    ThreadLocalContext.checkDomain(DestinationImpl.RESOURCE_TASK_KEY);
    if (key == retainedIdentifier) {
      totalRetained.decrement();
      retainedIdentifier = -1;
    }
  }

  public abstract long size() throws IOException;

  public long getRetainedIdentifier() {
    return retainedIdentifier;
  }

  public abstract void stop() throws IOException;
}
