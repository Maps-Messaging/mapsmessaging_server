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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.utils.FilePathHelper;
import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.FileBitSetFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public class RetainManager {

  private static final Object UNINITIALIZED = new Object();
  private final Queue<Long> retainIndex;
  private final AtomicReference<Object> retainId;
  private final BitSetFactory factory;

  public RetainManager(boolean isPersistent, String path) throws IOException {
    factory = createFactory(path, isPersistent);
    retainIndex = new NaturalOrderedLongQueue(0, factory);
    retainId = new AtomicReference<>(UNINITIALIZED);
  }

  /** Null means that no retained message exists; negative IDs are valid. */
  public Long current() {
    Object value = retainId.get();
    if (value == UNINITIALIZED) {
      Long restored = retainIndex.peek();
      retainId.compareAndSet(UNINITIALIZED, restored);
      value = retainId.get();
    }
    return (Long) value;
  }

  /** Transfers ownership of the retained identifier; null clears the current retain. */
  public Long replace(Long newRetainId) {
    Long old = retainIndex.poll();
    if (newRetainId != null) {
      retainId.set(newRetainId);
      retainIndex.offer(newRetainId);
    } else {
      retainId.set(null);
    }
    return old;
  }

  private BitSetFactory createFactory(String path, boolean persistent) throws IOException {
    if (persistent) {
      String fullyQualifiedPath = FilePathHelper.cleanPath(path);
      File directory = new File(fullyQualifiedPath);
      if(!directory.exists()) {
        Files.createDirectories(directory.toPath());
      }
      fullyQualifiedPath = FilePathHelper.cleanPath(fullyQualifiedPath + File.separator + "retain.bin");
      return new FileBitSetFactoryImpl(fullyQualifiedPath, Constants.BITSET_BLOCK_SIZE);
    } else {
      return new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    }
  }

  public void close() throws IOException {
    factory.close();
  }
}
