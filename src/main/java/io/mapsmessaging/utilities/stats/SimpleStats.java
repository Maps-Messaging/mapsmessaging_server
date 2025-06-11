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

import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class SimpleStats implements Stats{

  @Getter
  private final String name;
  @Getter
  private final String units;
  private final long timeSpan;
  private long value;
  private float perSecond;
  private long previousSum;
  private final LongAdder current;
  private long nextProcessed;

  public SimpleStats(String name, int timeList, TimeUnit unit, String unitName) {
    this.name = name;
    this.timeSpan = unit.toMillis(timeList);
    this.units = unitName;
    current = new LongAdder();
    reset();
  }

  @Override
  public void reset() {
    if(nextProcessed +timeSpan > System.currentTimeMillis()) {
     // return;
    }
    value = 0;
    current.reset();
    perSecond = 0;
    previousSum = 0;
    nextProcessed = System.currentTimeMillis();
  }

  @Override
  public float getPerSecond() {
    process();
    return perSecond;
  }

  @Override
  public long getCurrent() {
    return value;
  }

  @Override
  public long getTotal() {
    process();
    return current.sum();
  }

  @Override
  public void add(long data) {
    value = data;
    current.add(data);
  }

  @Override
  public void subtract(long value) {
    current.add(value*-1);
  }

  private synchronized void process() {
    long currentTime = System.currentTimeMillis();
    if((nextProcessed+timeSpan) < currentTime) {
      double duration = (currentTime - nextProcessed);
      long t = current.sum();
      duration = duration/1000L;
      if(duration == 0){
        duration = timeSpan;
      }
      perSecond = (float) ((t-previousSum)/duration);
      nextProcessed = currentTime;
      previousSum = t;
    }
  }

  @Override
  public String toString(){
    return name+" "+getCurrent()+" "+getUnits()+" "+getPerSecond()+" "+getUnits()+"/sec"+" Total: "+getTotal()+" "+getUnits();
  }
}
