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

package io.mapsmessaging.utilities.collections.bitset;

import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class BitSetJMH {
  @State(Scope.Benchmark)
  public static class Index {
    public BitSet bitSet;
    {
      ByteBuffer bb = ByteBuffer.allocate(1024);
      bb.putLong(0, 0); // Set Offset
      bitSet = new ByteBufferBackedBitMap(bb, 0);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void checkSet(Index index){
    int x = (int)(System.currentTimeMillis() % 1024);
    index.bitSet.set(x);
  }


  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void checkSetAndTest(Index index){
    int x = (int)(System.currentTimeMillis() % 1024);
    index.bitSet.set(x);
    index.bitSet.isSet(x);
  }


  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void checkSetAndClear(Index index){
    int x = (int)(System.currentTimeMillis() % 1024);
    index.bitSet.set(x);
    index.bitSet.isSetAndClear(x);
  }



  public void testPerformance(BitSet bitmap){
    long start = System.currentTimeMillis();
    int len = bitmap.length();
    Random rdm = new Random();
    int bit = 0;
    while(bit < len){
      bitmap.set(Math.abs(rdm.nextInt())%bitmap.length());
      bit++;
    }
    System.err.println("time to randomly set "+len+" "+(System.currentTimeMillis()-start)+"ms");

    bitmap.clear();
    start = System.currentTimeMillis();
    bit = 0;
    while(bit < len){
      bitmap.set(bit);
      bit++;
    }
    start = System.currentTimeMillis()- start;
    System.err.println("time to set "+bitmap.length()+" "+start+"ms");
    Assertions.assertEquals(len, bitmap.cardinality() );

    start = System.currentTimeMillis();
    bit = 0;
    while(bit < len){
      bitmap.clear(bit);
      bit++;
    }
    System.err.println("time to clear "+bitmap.length()+" "+(System.currentTimeMillis()-start)+"ms");
    Assertions.assertEquals(0L, bitmap.cardinality() );
  }
}
