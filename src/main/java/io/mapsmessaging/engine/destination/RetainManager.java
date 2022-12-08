package io.mapsmessaging.engine.destination;

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.utils.FilePathHelper;
import io.mapsmessaging.utilities.collections.NaturalOrderedCollection;
import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.FileBitSetFactoryImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class RetainManager {

  private final Queue<Long> retainIndex;
  private final AtomicLong retainId;
  private final BitSetFactory factory;

  public RetainManager(boolean isPersistent, String path) throws IOException {
    factory = createFactory(path, isPersistent);
    retainIndex = new NaturalOrderedLongQueue(0, factory);
    retainId = new AtomicLong(-2);
  }

  public long current() {
    long res = retainId.get();
    if(res < -1) {
      Long result = retainIndex.peek();
      if(result != null) {
        retainId.set(result);
        return result;
      }
      else{
        retainId.set(-1);
        return -1;
      }
    }
    return res;
  }

  public long replace(long newRetainId) {
    Long old = retainIndex.poll();
    if (newRetainId != -1) {
      retainId.set(newRetainId);
      retainIndex.offer(newRetainId);
    }
    return old != null ? old : -1;
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
    ((NaturalOrderedCollection)retainIndex).close();
    factory.close();
  }
}
