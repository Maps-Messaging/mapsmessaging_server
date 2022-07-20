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

public class RetainManager {

  protected final Queue<Long> retainIndex;

  public RetainManager(boolean isPersistent, String path) throws IOException {
    BitSetFactory bitSetFactory = createFactory(path, isPersistent);
    retainIndex = new NaturalOrderedLongQueue(0, bitSetFactory);
  }

  public long current() {
    Long result = retainIndex.peek();
    return result != null ? result : -1;
  }

  public long replace(long newRetainId) {
    Long old = retainIndex.poll();
    if (newRetainId != -1) {
      retainIndex.offer(newRetainId);
    }
    return old != null ? old : -1;
  }

  private BitSetFactory createFactory(String path, boolean persistent) throws IOException {
    if (persistent) {
      String fullyQualifiedPath = FilePathHelper.cleanPath(path);
      File directory = new File(fullyQualifiedPath);
      Files.createDirectories(directory.toPath());
      fullyQualifiedPath = FilePathHelper.cleanPath(fullyQualifiedPath + File.separator + "retain.bin");
      return new FileBitSetFactoryImpl(fullyQualifiedPath, Constants.BITSET_BLOCK_SIZE);
    } else {
      return new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);
    }
  }
}
