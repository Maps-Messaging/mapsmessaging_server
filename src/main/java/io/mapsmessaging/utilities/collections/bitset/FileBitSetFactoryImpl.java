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

package io.mapsmessaging.utilities.collections.bitset;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import io.mapsmessaging.utilities.MappedBufferHelper;

public class FileBitSetFactoryImpl extends BitSetFactory {

  private static final int BITS_PER_BYTE = 8;
  private static final int HEADER_SIZE = 16;

  private final List<FileOffsetBitSet> free;
  private final List<FileOffsetBitSet> used;

  private final String filename;
  private final RandomAccessFile raf;
  private final int bufferSize;
  private final byte[] emptyBuffer;

  public FileBitSetFactoryImpl(@NonNull @NotNull String filename, int size) throws IOException {
    super(size);
    this.filename = filename;
    bufferSize = size / BITS_PER_BYTE;
    free = new ArrayList<>();
    used = new ArrayList<>();
    File testFile = new File(filename);
    File parent = testFile.getParentFile();
    if (parent != null) {
      Files.createDirectories(testFile.getParentFile().toPath());
    }

    raf = new RandomAccessFile(testFile, "rw");
    emptyBuffer = new byte[bufferSize];
    for (int x = 0; x < bufferSize; x++) {
      emptyBuffer[x] = 0;
    }
    long len = raf.length();
    long pos = 0;
    while (pos < len) {
      raf.seek(pos);
      long uniqueId = raf.readLong();
      long startId = raf.readLong();
      pos += HEADER_SIZE;
      ByteBufferBackedBitMap bitmap = map(pos, uniqueId);
      free.add(new FileOffsetBitSet(bitmap, pos - HEADER_SIZE, startId, this));
      pos += bufferSize;
    }
  }

  @Override
  public void delete() throws IOException {
    close();
    File check = new File(filename);
    if (check.exists()) {
      Files.delete(check.toPath());
    }
  }

  @Override
  public void close() throws IOException {
    clearList(used);
    clearList(free);
    raf.close();
  }

  private void clearList(@NonNull @NotNull List<FileOffsetBitSet> list) {
    for (FileOffsetBitSet bitmap : list) {
      ByteBufferBackedBitMap mapped = (ByteBufferBackedBitMap) bitmap.getBitSet();
      bitmap.releaseBitSet();
      unmap(mapped);
    }
    list.clear();
  }

  public String getFileName() {
    return filename;
  }

  @Override
  public OffsetBitSet open(long uniqueId, long id) throws IOException {
    FileOffsetBitSet response;
    long startId = getStartIndex(id);
    if (free.isEmpty()) {
      long end = raf.length();
      raf.seek(end);
      raf.writeLong(uniqueId);
      raf.writeLong(startId);
      raf.write(emptyBuffer);
      ByteBufferBackedBitMap bitmap = map(end + HEADER_SIZE, uniqueId);
      response = new FileOffsetBitSet(bitmap, end, startId, this);
    } else {
      response = free.remove(0);
      raf.seek(response.getPosition());
      raf.writeLong(uniqueId);
      raf.writeLong(startId);
      response.reset(startId, uniqueId);
    }
    used.add(response);
    return response;
  }

  @Override
  public void close(@NonNull @NotNull OffsetBitSet bitset) {
    bitset.reset(0, -1);
    free.add((FileOffsetBitSet) bitset);
    used.remove(bitset);
  }

  @Override
  public List<OffsetBitSet> get(long uniqueId) {
    List<OffsetBitSet> response = new ArrayList<>();
    Iterator<FileOffsetBitSet> freeList = free.iterator();
    while (freeList.hasNext()) {
      FileOffsetBitSet bitset = freeList.next();
      if (bitset.getUniqueId() == uniqueId) {
        freeList.remove();
        used.add(bitset);
        response.add(bitset);
      }
    }
    return response;
  }

  @Override
  public List<Long> getUniqueIds() {
    List<Long> response = new ArrayList<>();
    for (FileOffsetBitSet bitset : free) {
      if (!response.contains(bitset.getUniqueId())) {
        response.add(bitset.getUniqueId());
      }
    }
    return response;
  }

  private ByteBufferBackedBitMap map(long pos, long uniqueId) throws IOException {
    return new ByteBufferBackedBitMap(raf.getChannel().map(MapMode.READ_WRITE, pos, bufferSize), 0, uniqueId);
  }

  private void unmap(@NonNull @NotNull ByteBufferBackedBitMap mapped) {
    ByteBuffer backing = mapped.clearBacking();
    MappedBufferHelper.closeDirectBuffer(backing);
  }

  private static class FileOffsetBitSet extends OffsetBitSet implements AutoCloseable {

    private final BitSetFactory factory;
    private final long position;

    public FileOffsetBitSet(@NonNull @NotNull ByteBufferBackedBitMap bitSet, long position, long offset, @NonNull @NotNull BitSetFactory factory) {
      super(bitSet, offset);
      this.factory = factory;
      this.position = position;
    }

    public long getPosition() {
      return position;
    }

    public long getUniqueId() {
      return getBitSet().getUniqueId();
    }

    @Override
    public void close() {
      factory.close(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof OffsetBitSet) {
        return compareTo((OffsetBitSet) obj) == 0;
      }
      return super.equals(obj);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

}
