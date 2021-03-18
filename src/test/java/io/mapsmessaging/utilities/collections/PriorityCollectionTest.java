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

package io.mapsmessaging.utilities.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PriorityCollectionTest {


  @Test
  void constructors() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new PriorityCollection<Long>(-1, null));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new PriorityCollection<Long>(0, null));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new PriorityCollection<Long>(null, null));

    Assertions.assertDoesNotThrow(()->new PriorityCollection<Long>(1, null));
    Assertions.assertDoesNotThrow(()->new PriorityCollection<Long>(1024, null));

    Assertions.assertDoesNotThrow(()->new PriorityCollection<Long>(1, new PriorityFactoryTest<>()));
    Assertions.assertDoesNotThrow(()->new PriorityCollection<Long>(1024, new PriorityFactoryTest<>()));

    Queue<Long>[] queues = new Queue[16];
    for(int x=0;x<queues.length;x++){
      queues[x] = new LinkedList<>();
    }
    Assertions.assertDoesNotThrow(()->new PriorityCollection<Long>(queues, null));
    Assertions.assertDoesNotThrow(()->new PriorityCollection<Long>(queues, new PriorityFactoryTest<>()));
  }

  @Test
  void addAllToCollection(){
    PriorityCollection<Long> collection = new PriorityCollection<Long>(16, new PriorityFactoryTest<>());
    for(long x=0;x<4096;x++){
      collection.add(x, (int)x%16);
    }

    PriorityCollection<Long> collection2 = new PriorityCollection<Long>(16, null);
    collection2.addAll(collection);
    Assertions.assertEquals(4096, collection.size());
    Assertions.assertEquals(collection2.size(), collection.size());
    Iterator<Long> iterator1 = collection.iterator();
    Iterator<Long> iterator2 = collection2.iterator();
    while(iterator1.hasNext() && iterator2.hasNext()){
      Assertions.assertEquals(iterator1.next(), iterator2.next());
    }
    collection.clear();
    collection2.clear();
    Assertions.assertTrue(collection.isEmpty());
    Assertions.assertTrue(collection2.isEmpty());

    Queue<Long> standard = new LinkedList<>();
    for(long x=0;x<4096;x++){
      standard.add(x);
    }
    collection.addAll(standard);
    Assertions.assertFalse(collection.isEmpty());
    long expecting = 0;
    iterator1 = collection.iterator();
    while(iterator1.hasNext()){
      Assertions.assertEquals(expecting++, iterator1.next());
    }

  }

  @Test
  void testToString(){
    PriorityCollection<Long> collection = new PriorityCollection<Long>(16, null);
    for(long x=0;x<16;x++){
      collection.add(x, 15-(int)x);
    }
    String shouldBe = "Count:16,[15]\n[14]\n[13]\n[12]\n[11]\n[10]\n[9]\n[8]\n[7]\n[6]\n[5]\n[4]\n[3]\n[2]\n[1]\n[0]\n";
    Assertions.assertEquals(shouldBe, collection.toString());
    collection.clear();
    for(long x=0;x<16;x++){
      collection.add(x, (int)x);
    }
    shouldBe = "Count:16,[0]\n[1]\n[2]\n[3]\n[4]\n[5]\n[6]\n[7]\n[8]\n[9]\n[10]\n[11]\n[12]\n[13]\n[14]\n[15]\n";
    Assertions.assertEquals(shouldBe, collection.toString());
  }

  @Test
  void allAPICollections(){
    List<Long> list = new ArrayList<>();
    for(long x=0;x< 1000;x++){
      list.add(x);
    }
    PriorityCollection<Long> collection = new PriorityCollection<Long>(16, null);
    collection.addAll(list, 2);
    Assertions.assertEquals(1000, collection.size());

    Assertions.assertTrue(collection.containsAll(list));

    Assertions.assertFalse(collection.retainAll(list));
    Assertions.assertEquals(1000, collection.size());

    collection.removeAll(list);
    Assertions.assertEquals(0, collection.size());

    collection.addAll(list, 4);
    Assertions.assertEquals(1000, collection.size());

    collection.clear();
    Assertions.assertEquals(0, collection.size());

    collection.addAll(list, 8);
    list.clear();
    for(long x=0;x<1000;x++){
      if(x%2 == 0){
        list.add(x);
      }
    }
    collection.retainAll(list);
    Assertions.assertEquals(list.size(), collection.size());
    Assertions.assertTrue(collection.containsAll(list));
  }

  @Test
  void priorityToFlattened(){
    PriorityCollection<Long> collection = new PriorityCollection<Long>(16, null);
    for(long x=0;x<4096;x++){
      collection.add(x, (int)x%16);
    }
    Assertions.assertEquals(4096, collection.size());
    Queue<Long> flattened = new LinkedList<>();
    flattened = collection.flatten(flattened);
    Assertions.assertEquals(4096, flattened.size());

    long expecting = 0;
    long priorityLevel =0;
    int count =0;
    while(!flattened.isEmpty()){
      Assertions.assertEquals(expecting, flattened.remove());
      expecting += 16;
      count++;
      if(count == 256){
        count =0;
        priorityLevel++;
        expecting = priorityLevel;
      }
    }
  }


  @Test
  void containsEntry() {
    PriorityCollection<Long> collection = new PriorityCollection<Long>(16, null);
    for (long x = 0; x < 4096; x++) {
      collection.add(x, (int) x % 16);
    }
    Assertions.assertEquals(4096, collection.size());
    for (long x = 0; x < 4096; x++) {
      Assertions.assertTrue(collection.contains(x));
    }
    Assertions.assertFalse(collection.contains(4097L));
  }

  @Test
  void toArray() {
    PriorityCollection<Long> collection = new PriorityCollection<Long>(16, null);
    for (long x = 0; x < 4096; x++) {
      collection.add(x, (int) x % 16);
    }

    Object[] array = collection.toArray();
    Assertions.assertEquals(4096, array.length);
    long expecting = 15;
    long priorityLevel = 15;
    int count =0;
    for (Object o : array) {
      Assertions.assertEquals(expecting, o);
      expecting += 16;
      count++;
      if (count == 256) {
        count = 0;
        priorityLevel--;
        expecting = priorityLevel;
      }
    }

    array = collection.toArray(array);
    Assertions.assertEquals(4096, array.length);
    expecting = 15;
    priorityLevel = 15;
    count =0;
    for (Object o : array) {
      Assertions.assertEquals(expecting, o);
      expecting += 16;
      count++;
      if (count == 256) {
        count = 0;
        priorityLevel--;
        expecting = priorityLevel;
      }
    }

    collection.clear();
    for(int priority=0;priority<16;priority++) {
      for (long x = 0; x < 4096; x++) {
        collection.add(x, priority);
      }
      array = collection.toArray(array);
      Assertions.assertEquals(4096, array.length);

      expecting = 0;
      for (Object o : array) {
        Assertions.assertEquals(expecting, o);
        expecting += 1;
      }
      collection.clear();
    }
  }



  static class PriorityFactoryTest<T> implements PriorityFactory<T>{

    @Override
    public int getPriority(T value) {
      return 1;
    }
  }
}
