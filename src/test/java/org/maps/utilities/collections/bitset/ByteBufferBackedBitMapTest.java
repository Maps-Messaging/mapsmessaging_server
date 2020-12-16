/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.utilities.collections.bitset;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ByteBufferBackedBitMapTest extends BitSetTest{

  @Test
  public void testOffsetLogic(){
    ByteBuffer bb = ByteBuffer.allocate(1024);
    bb.putLong(0, 32); // Set Offset
    BitSet bitset = new ByteBufferBackedBitMap(bb, 32);
    Assertions.assertEquals((1024-32)*8, bitset.length());
    bitset.set(0);
    Assertions.assertTrue(bitset.isSet(0));

    for(int x=0;x<bitset.length();x++){
      bitset.set(x);
      Assertions.assertTrue(bitset.isSet(x));
    }
    for(int x=0;x<bitset.length();x++){
      bitset.clear(x);
      Assertions.assertFalse(bitset.isSet(x));
    }

    try{
      bitset.set(1024*8-1);
      Assertions.fail("Should have thrown an exception");
    }
    catch(Exception ex){
      // should fail
    }
    bitset.set(64);
    Assertions.assertEquals(64, bitset.nextSetBit(0));
  }

  @Test
  public void testDiskPerformance(){
    long bufferSize = 1024L*1024L;
    File file = new File("test.dmp");
    try {
      if(file.exists()){
        file.delete();
      }
      file.createNewFile();
      byte[] buffer = new byte[1024];
      IntStream.range(0, buffer.length).forEach(x -> buffer[x] = 0);
      RandomAccessFile raf = new RandomAccessFile(file, "rw");
      for(int x=0;x<1024;x++) {
        raf.write(buffer);
      }
      ByteBuffer bb = raf.getChannel().map(MapMode.READ_WRITE, 0, bufferSize);
      ByteBufferBackedBitMap bitMap = new ByteBufferBackedBitMap(bb, 0);
      testPerformance(bitMap);
      raf.close();
      file.deleteOnExit();
    } catch (IOException e) {
      Assertions.fail(e);
    }
  }

  @Override
  public BitSet getBitSet(int size) {
    ByteBuffer bb = ByteBuffer.allocate(size);
    bb.putLong(0, 0); // Set Offset
    BitSet bitset = new ByteBufferBackedBitMap(bb, 0);
    return bitset;
  }

}
