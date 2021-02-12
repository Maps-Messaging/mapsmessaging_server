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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.maps.BaseTest;

abstract class PriorityBitSetQueueTest extends BaseTest {

  private static final int RUN_TIME = 1000;

  int[][] insertionTests = {{10,2}, {1000,2}, {1000, 4}, {1000, 10}, {1000, 20}};

  public abstract PriorityQueue<Long> createQueue(int priorities);


  @AfterEach
  void tearDown() throws Exception {

  }

  @Test
  @DisplayName("Test multiple add/remove of the same value")
  void testMultipleInsertionDeletion() throws Exception {
    for(int[] values:insertionTests) {
      insertionValidationDuplicates(values[0], values[1], 5);
    }
  }

  @Test
  @DisplayName("Test simple insertions")
  void testSimpleInsertion() throws Exception {
    for(int[] values:insertionTests) {
      insertionValidation(values[0], values[1]);
    }
  }

  @Test
  @DisplayName("Test simple insertions and walk the structure")
  void testSimpleInsertionAndWalk() throws Exception {
    for(int[] values:insertionTests) {
      insertionAndWalkValidation(values[0], values[1]);
    }
  }

  @Test
  @DisplayName("Test simple insertions and remove the entries")
  void testSimpleInsertionAndDrain() throws Exception {
    for(int[] values:insertionTests) {
      insertionAndDrainValidation(values[0], values[1]);
    }
  }

  @Test
  @DisplayName("Test simple insertions, remove a priority level")
  void testLimitedInsertionAndDrain() throws Exception {
    for(int[] values:insertionTests) {
      limitedInsertionAndDrain(values[0], values[1]);
    }
  }

  @Test
  @DisplayName("Test simple insertions")
  void testSimpleInsertionExternal() throws Exception {
    for(int[] values:insertionTests) {
      insertionValidation(values[0], values[1]);
    }
  }

  @Test
  void testSimpleInsertionAndWalkExternal() throws Exception {
    for(int[] values:insertionTests) {
      insertionAndWalkValidation(values[0], values[1]);
    }
  }

  @Test
  void testSimpleInsertionAndDrainExternal() throws Exception {
    for(int[] values:insertionTests) {
      insertionAndDrainValidation(values[0], values[1]);
    }
  }

  @Test
  void testLimitedInsertionAndDrainExternal() throws Exception {
    for(int[] values:insertionTests) {
      limitedInsertionAndDrain(values[0], values[1]);
    }
  }

  @Test
  void testRandomEntries() throws Exception {
    try(PriorityQueue<Long> priorityQueue = createQueue(16)) {
      List<ArrayList<Long>>  comparison = new ArrayList<>();
      for(int x=0;x<16;x++){
        comparison.add(new ArrayList<>());
      }
      Random rdm = new Random();
      long endTime = System.currentTimeMillis()+ RUN_TIME;
      int count = 0;

      while(endTime > System.currentTimeMillis() && count < 1000000){
        long value = Math.abs(rdm.nextLong()%100000000L);
        int priority = Math.abs(rdm.nextInt(16));
        ArrayList<Long> pq = comparison.get(priority);
        if(!pq.contains(value)){
          priorityQueue.add(value, priority);
          pq.add(value);
          count++;
        }
      }
      System.err.println("Added "+priorityQueue.size()+" in "+(RUN_TIME/1000)+"seconds");
      for(int x=0;x<16;x++){
        comparison.get(x).sort(Long::compare);
        Assertions.assertEquals(priorityQueue.priorityStructure.get(x).size(), comparison.get(x).size());
      }

      long testSize = 0;
      for(int x=15;x>=0;x--){
        testSize += comparison.get(x).size();
      }
      Assertions.assertEquals(priorityQueue.size(), count);
      Assertions.assertEquals(testSize, count);
      Iterator<Long> priorityIterator = priorityQueue.iterator();
      Iterator<Long> stagedIterator = comparison.remove(comparison.size()-1).iterator();
      while(priorityIterator.hasNext()){
        if(!stagedIterator.hasNext()){
          if(comparison.isEmpty()){
            Assertions.fail("We still have entries");
          }
          stagedIterator = comparison.remove(comparison.size()-1).iterator();
        }
        long test1 = priorityIterator.next();
        long test2 = stagedIterator.next();
        Assertions.assertEquals(test1, test2);
      }
    }
  }

  @Test
  void testLinearEntries() throws Exception{
    try ( PriorityQueue<Long> priorityQueue = createQueue(16)){
      List<ArrayList<Long>>  comparison = new ArrayList<>();
      for(int x=0;x<16;x++){
        comparison.add(new ArrayList<>());
      }
      long endTime = System.currentTimeMillis()+ RUN_TIME;
      long count = 0;

      while(endTime > System.currentTimeMillis() && count < 1000000000L){
        long value = count;
        int priority = ((int)count % 16);
        ArrayList<Long> pq = comparison.get(priority);
        priorityQueue.add(value, priority);
        pq.add(value);
        count++;
      }
      System.err.println("Added "+priorityQueue.size()+" in"+(RUN_TIME/1000)+"seconds");
      for(int x=0;x<16;x++){
        Assertions.assertEquals(priorityQueue.priorityStructure.get(x).size(), comparison.get(x).size());
      }

      long testSize = 0;
      for(int x=15;x>=0;x--){
        testSize += comparison.get(x).size();
      }
      Assertions.assertEquals(priorityQueue.size(), count);
      Assertions.assertEquals(testSize, count);
      Iterator<Long> priorityIterator = priorityQueue.iterator();
      Iterator<Long> stagedIterator = comparison.remove(comparison.size()-1).iterator();
      while(priorityIterator.hasNext()){
        if(!stagedIterator.hasNext()){
          if(comparison.isEmpty()){
            Assertions.fail("We still have entries");
          }
          stagedIterator = comparison.remove(comparison.size()-1).iterator();
        }
        long test1 = priorityIterator.next();
        long test2 = stagedIterator.next();
        Assertions.assertEquals(test1, test2);
      }
    }
  }

  private void limitedInsertionAndDrain(int entries, int priorities) throws Exception {
    try (PriorityQueue<Long> priorityQueue = createAndInsert(entries, priorities)) {
      for (int x = 0; x < priorities; x++) {
        Queue<Long> list = priorityQueue.priorityStructure.get(x);
        Assertions.assertEquals(entries / priorities, list.size());
        Long uniqueIdStart = list.peek();
        for (Long testData : list) {
          Assertions.assertEquals(uniqueIdStart, testData);
          uniqueIdStart += priorities;
        }
      }

      //
      // Remove a complete priority set
      //
      int removed = priorities / 2;
      Long[] queue = new Long[priorityQueue.priorityStructure.get(removed).size()];
      queue = priorityQueue.priorityStructure.get(removed).toArray(queue);
      for(Long l:queue){
        priorityQueue.remove(l);
      }
      int size = entries - (entries / priorities);
      Assertions.assertEquals(size, priorityQueue.size());

      //
      // The structure of the queue seems fine, so now lets destructively read it, we should walk backwards from the priority
      // Need to calculate the first priority level we should expect and the unique ID we should expect.
      //

      int priorityStart = priorities - 1;
      if (priorityStart == removed) {
        priorityStart--;
      }
      int uniqueIdStart = priorityStart;
      int counter = 0;
      int overallCounter = 0;
      while (!priorityQueue.isEmpty()) {
        Long testData = priorityQueue.poll();
        Assertions.assertNotNull(testData);
        Assertions.assertEquals(testData, uniqueIdStart);
        overallCounter++;
        counter++;
        uniqueIdStart += priorities;
        if (counter == entries / priorities) {
          counter = 0;
          priorityStart--;
          if (priorityStart == removed) {
            priorityStart--;
          }

          uniqueIdStart = priorityStart;
        }
      }

      Assertions.assertEquals(size, overallCounter);
      Assertions.assertEquals(0, priorityQueue.size());
    }
  }

  private void insertionAndDrainValidation(int entries, int priorities) throws Exception {
    try (PriorityQueue<Long> priorityQueue = createAndInsert(entries, priorities)) {
      for (int x = 0; x < priorities; x++) {
        Queue<Long> list = priorityQueue.priorityStructure.get(x);
        Assertions.assertEquals(entries / priorities, list.size());
        Long uniqueIdStart = list.peek();
        for (Long testData : list) {
          Assertions.assertEquals(uniqueIdStart, testData);
          uniqueIdStart += priorities;
        }
      }
      //
      // The structure of the queue seems fine, so now lets destructively read it, we should walk backwards from the priority
      // Need to calculate the first priority level we should expect and the unique ID we should expect.
      //

      int priorityStart = priorities - 1;
      int uniqueIdStart = priorityStart;
      int counter = 0;
      int overallCounter = 0;
      while (!priorityQueue.isEmpty()) {
        Long testData = priorityQueue.poll();
        Assertions.assertNotNull(testData);
        Assertions.assertEquals(testData, uniqueIdStart);
        overallCounter++;
        counter++;
        uniqueIdStart += priorities;
        if (counter == entries / priorities) {
          counter = 0;
          priorityStart--;
          uniqueIdStart = priorityStart;
        }
      }

      Assertions.assertEquals(entries, overallCounter);
      Assertions.assertEquals(0, priorityQueue.size());
    }
  }

  private void insertionAndWalkValidation(int entries, int priorities) throws Exception {
    try (PriorityQueue<Long> priorityQueue = createAndInsert(entries, priorities)) {
      for (int x = 0; x < priorities; x++) {
        Queue<Long> list = priorityQueue.priorityStructure.get(x);
        Assertions.assertEquals(entries / priorities, list.size());
        long uniqueIdStart = list.peek();
        for (Long testData : list) {
          Assertions.assertEquals(uniqueIdStart, testData);
          uniqueIdStart += priorities;
        }
      }
    }
  }

  private void insertionValidation(int entries, int priorities) throws Exception {
    try (PriorityQueue<Long> priorityQueue = createAndInsert(entries, priorities)) {
      for (int x = 0; x < priorities; x++) {
        Queue<Long> list = priorityQueue.priorityStructure.get(x);
        Assertions.assertEquals(entries / priorities, list.size());
      }
    }
  }

  private void insertionValidationDuplicates(int entries, int priorities, int duplicates)
      throws Exception {
    try (PriorityQueue<Long> priorityQueue = createAndInsert(entries, priorities)) {
      for (int x = 0; x < priorities; x++) {
        Queue<Long> list = priorityQueue.priorityStructure.get(x);
        Assertions.assertEquals(entries / priorities, list.size());
      }
      int size = priorityQueue.size();
      for (long x = 0; x < entries; x++) {
        for (int y = 0; y < duplicates; y++) {
          priorityQueue.add(x, (int) x % priorities);
        }
      }
      Assertions.assertEquals(size, priorityQueue.size());

      for (long x = 0; x < entries; x++) {
        size = priorityQueue.size();
        for (int y = 0; y < duplicates; y++) {
          priorityQueue.remove(x);
        }
        Assertions.assertEquals(size-1, priorityQueue.size());
      }
      Assertions.assertEquals(0, priorityQueue.size());
    }
  }

  private PriorityQueue<Long> createAndInsert(int entries, int priorities){
    PriorityQueue<Long> priorityQueue = createQueue(priorities);
    try {
      Assertions.assertEquals(priorityQueue.priorityStructure.size(), priorities);
      for(long x=0;x<entries;x++){
        priorityQueue.add(x,(int)x%priorities);
      }
      Assertions.assertEquals(priorityQueue.size(), entries);
      return priorityQueue;
    } catch (Throwable e) {
      try {
        priorityQueue.close();
      } catch (Exception ex) {
        // Ignore for testing purposes
      }
      throw e;
    }
  }
}
