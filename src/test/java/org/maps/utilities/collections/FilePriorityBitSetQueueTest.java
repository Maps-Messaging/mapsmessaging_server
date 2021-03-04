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

package org.maps.utilities.collections;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.utilities.collections.bitset.FileBitSetFactoryImpl;

public class FilePriorityBitSetQueueTest extends PriorityBitSetQueueTest {

  static long counter =0;
  FileBitSetFactoryImpl factory;

  public FilePriorityBitSetQueueTest()  {
    try {
      File file = new File("test_file_bitset.queue_"+counter++);
      if(file.exists()){
        file.delete();
      }
      factory = new FileBitSetFactoryImpl(file.getName(),4096);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    File file = new File(factory.getFileName());
    factory.close();
    if(file.exists()){
      if(!file.delete()){
        System.err.println("Unabled to delete "+file.getName());
      }
    }

  }



  @Test
  public void testFileReload() throws Exception {
    File file = new File("test_file_bitset.queue_reload");
    if(file.exists()){
      file.delete();
    }
    FileBitSetFactoryImpl localFactory = new FileBitSetFactoryImpl(file.getName(),4096);
    PriorityQueue<Long> priorityList = createQueue(16, localFactory);
    for(int x=0;x<2048;x++){
      long t = x;
      priorityList.add(t, x%16);
    }
    localFactory.close();

    localFactory = new FileBitSetFactoryImpl(file.getName(),4096);
    priorityList = createQueue(16, localFactory);

    for(int y = 15;y>=0;y--) {
      for (int x = 0; x < 2048 / 16; x++) {
        Long val = priorityList.poll();
        if(val != null){
          Assertions.assertEquals( (y+(x*16)), val);
        }
      }
    }
    priorityList.close();
    localFactory.close();
    if(file.exists()){
      Assertions.assertTrue(file.delete());
    }
  }


  public PriorityQueue<Long> createQueue(int priorities){
    return createQueue(priorities, factory);
  }

  private  PriorityQueue<Long> createQueue(int priorities,  FileBitSetFactoryImpl bitsetFactory){
    Runtime.getRuntime().gc();
    Queue<Long>[] external = new Queue[priorities];

    System.gc();
    for(int x=0;x<priorities;x++){
      external[x] = new NaturalOrderedLongQueue(x, bitsetFactory);
    }
    return new PriorityQueue<>(external,null);
  }

}
