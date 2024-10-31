/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.utilities.stats;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;

public class SimpleStats implements Stats{

  @Getter
  private final String name;
  @Getter
  private final String units;
  private final long timeSpan;
  private long value;
  private long perSecond;
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
    value = 0;
    current.reset();
    perSecond = 0;
    previousSum = 0;
    nextProcessed = System.currentTimeMillis()+timeSpan;
  }

  @Override
  public long getPerSecond() {
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
    if(nextProcessed < currentTime) {
      long duration = (currentTime - (nextProcessed-timeSpan))/1000L;
      nextProcessed = currentTime + timeSpan;
      long t = current.sum();
      perSecond = (t-previousSum)/duration;
      previousSum = t;
    }
  }

  @Override
  public String toString(){
    return name+" "+getCurrent()+" "+getUnits()+" "+getPerSecond()+" "+getUnits()+"/sec"+" Total: "+getTotal()+" "+getUnits();
  }
}
