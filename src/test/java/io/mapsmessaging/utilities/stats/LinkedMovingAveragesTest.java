/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.utilities.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.test.WaitForState;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class LinkedMovingAveragesTest {

  private final static String NAME = "Test";
  private final static int START = 1;
  private final static int PERIODS = 10;
  private final static int TOTAL = 6;
  private final static TimeUnit SECONDS = TimeUnit.SECONDS;
  private final static String units = "testUnits";

  LinkedMovingAverages create(){
    return MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, NAME, START, PERIODS, TOTAL,  SECONDS, units);
  }

  @Test
  void basicFunctions(){
    // We have created a list of moving average buckets starting with 1 second, incrementing by 10 seconds for a total of 6 buckets
    LinkedMovingAverages linked = create();
    long epoch = System.currentTimeMillis() + 500;
    long count=0;
    while(epoch > System.currentTimeMillis()){
      linked.add(1);
      count++;
    }
    linked.update();
    int div = 1;
    for(MovingAverage movingAverage:linked.movingAverages){
      assertEquals(count/div, (movingAverage.getAverage()));
      if(div == 1)div = 0;
      div += 10;
    }
  }

  @Test
  void getName() {
    LinkedMovingAverages linked = create();
    assertEquals("Test", linked.getName());
  }

  @Test
  void getUnits() {
    LinkedMovingAverages linked = create();
    assertEquals("testUnits", linked.getUnits());
  }

  @Test
  void getNames() {
    LinkedMovingAverages linked = create();
    int idx = 1;
    for(String name:linked.getNames()){
      assertEquals(idx+"_"+SECONDS.toString(), name);
      if(idx == 1)idx=0;
      idx+=10;
    }
  }

  @Test
  void increment() {
    LinkedMovingAverages linked = create();
    for(int x=0;x<100;x++) {
      linked.increment();
    }
    linked.update();
    assertEquals(100, linked.getTotal());
  }

  @Test
  void decrement() {
    LinkedMovingAverages linked = create();
    for(int x=0;x<100;x++) {
      linked.increment();
    }
    for(int x=0;x<50;x++) {
      linked.decrement();
    }
    linked.update();
    assertEquals(50, linked.getTotal());
  }

  @Test
  void add() {
    LinkedMovingAverages linked = create();
    for(int x=0;x<100;x++) {
      linked.add(10);
    }
    linked.update();
    assertEquals(1000, linked.getTotal());
  }

  @Test
  void subtract() {
    LinkedMovingAverages linked = create();
    for(int x=0;x<100;x++) {
      linked.subtract(10);
    }
    linked.update();
    assertEquals(-1000, linked.getTotal());
  }

  @Test
  void getCurrent() {
    LinkedMovingAverages linked = create();
    assertEquals(0, linked.getCurrent());
    for(int x=0;x<100;x++) {
      linked.add(10);
    }
    assertEquals(10, linked.getCurrent());
    linked.update();
    assertEquals(1000, linked.getTotal());
    assertEquals(10, linked.getCurrent());
  }

  @Test
  void getAverage() throws IOException {
    LinkedMovingAverages linked = create();
    for(int x=0;x<10000;x++) {
      linked.increment();
    }
    long epoch = System.currentTimeMillis()+1100;
    WaitForState.waitFor(1, TimeUnit.SECONDS, ()-> epoch <= System.currentTimeMillis());
    linked.update();
    int div = 1;
    for(String name:linked.getNames()){
      String lookup =(name);
      if(div == 1){
        assertEquals(10000, linked.getAverage(lookup));
        div=0;
      }
      else {
        div += 10;
        assertEquals(10000/div, linked.getAverage(lookup));
      }
    }
  }

  @Test
  void reset() {
    LinkedMovingAverages linked = create();
    for(int x=0;x<100;x++) {
      linked.add(10);
    }
    linked.update();
    assertEquals(1000, linked.getTotal());
    linked.reset();
    assertEquals(0, linked.getTotal());
    assertEquals(0, linked.getCurrent());
  }
}