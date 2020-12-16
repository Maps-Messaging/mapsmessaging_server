/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.utilities.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.maps.utilities.stats.processors.DataProcessor;

public class LinkedMovingAverages {

  protected final List<MovingAverage> movingAverages;
  private final DataProcessor dataProcessor;
  private final LongAdder total;
  private final String name;
  private final String unitName;

  private long previous;
  private long lastUpdate;
  private long timeSpan;

  public LinkedMovingAverages (DataProcessor dataProcessor, String name, int[] timeList, TimeUnit unit, String unitName){
    this.dataProcessor = dataProcessor;
    this.name = name;
    this.unitName = unitName;
    total = new LongAdder();
    movingAverages = new ArrayList<>();
    int start = 0;
    for(int time:timeList){
      movingAverages.add(new MovingAverage(time- start, unit));
      if(start == 0){
        start = time;
      }
    }
    previous = 0;
    lastUpdate = 0L;
    timeSpan = unit.toMillis(timeList[0]);
  }

  public void close(){
    MovingAverageFactory.getInstance().close(this);
  }

  public String getName(){
    return name;
  }

  public String getUnits(){
    return unitName;
  }

  public String[] getNames(){
    String[] names = new String[movingAverages.size()];
    int x =0;
    for(MovingAverage movingAverage:movingAverages){
      names[x] = movingAverage.getName();
      x++;
    }
    return names;
  }

  public void increment(){
    add(1);
  }

  public void decrement(){
    add(-1);
  }

  public void add(long value){
    total.add(dataProcessor.add(value, previous));
    previous = value;
  }

  public void subtract(long value){
    total.add(dataProcessor.add(value *-1, previous));
    previous = value*-1;
  }

  public long getCurrent(){
    return previous;
  }

  public long getTotal(){
    return total.sum();
  }

  public long[] getAverages(){
    long[] response = new long[movingAverages.size()];
    int index =0;
    for(MovingAverage average:movingAverages){
      response[index] = average.getAverage();
      index++;
    }
    return response;
  }

  public long getAverage(String name){
    for(MovingAverage average:movingAverages){
      if(average.getName().equals(name)){
        return average.getAverage();
      }
    }
    return -1;
  }


  public String toString(){
    StringBuilder sb = new StringBuilder(getClass().getName()+":");
    for(MovingAverage movingAverage:movingAverages){
      sb.append(movingAverage.getName()).append("=").append(movingAverage.getAverage()).append(",");
    }
    return sb.toString();
  }

  protected void update(){
    if(lastUpdate < System.currentTimeMillis()) {
      lastUpdate = System.currentTimeMillis() + timeSpan;
      long ave = dataProcessor.calculate();
      for (MovingAverage movingAverage : movingAverages) {
        movingAverage.add(ave);
      }
    }
  }

  public void reset() {
    total.reset();
    dataProcessor.reset();
    for(MovingAverage average:movingAverages){
      average.reset();
    }
  }

}
