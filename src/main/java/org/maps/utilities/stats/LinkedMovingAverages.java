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

package org.maps.utilities.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.maps.utilities.stats.processors.DataProcessor;

/**
 * Creates a list of moving average with different time periods. Data is pushed through all the moving averages resulting in the ability
 * to see longer period trends versus short term trends.
 */
public class LinkedMovingAverages {

  protected final List<MovingAverage> movingAverages;
  private final DataProcessor dataProcessor;
  private final LongAdder total;
  private final String name;
  private final String unitName;

  private long previous;
  private long lastUpdate;
  private long timeSpan;

  /**
   * Creates an instance of a list of moving averages
   *
   * @param dataProcessor  The data processor object to use when adding data to the average
   * @param name The name of this instance
   * @param timeList A List of time intervals to create moving averages for
   * @param unit The TimeUnit that represents the time units provied in timeList
   * @param unitName The name of the unit being measured
   */
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

  /**
   * Closes and clears all instances of moving objects
   */
  public void close(){
    MovingAverageFactory.getInstance().close(this);
  }

  /**
   *
   * @return The name of this instance
   */
  public String getName(){
    return name;
  }

  /**
   *
   * @return The name of the units being measured
   */
  public String getUnits(){
    return unitName;
  }

  /**
   *
   * @return A list of complete names of all moving averages
   */
  public String[] getNames(){
    String[] names = new String[movingAverages.size()];
    int x =0;
    for(MovingAverage movingAverage:movingAverages){
      names[x] = movingAverage.getName();
      x++;
    }
    return names;
  }

  /**
   * Increment the current value
   */
  public void increment(){
    add(1);
  }

  /**
   * Decrement the current value
   */
  public void decrement(){
    add(-1);
  }

  /**
   * @param value Add the supplied value to the current average
   */
  public void add(long value){
    total.add(dataProcessor.add(value, previous));
    previous = value;
  }

  /**
   *
   * @param value Subtract the supplied value from the current average
   */
  public void subtract(long value){
    total.add(dataProcessor.add(value *-1, previous));
    previous = value*-1;
  }

  /**
   *
   * @return The current value
   */
  public long getCurrent(){
    return previous;
  }

  /**
   *
   * @return The running total for the current period
   */
  public long getTotal(){
    return total.sum();
  }

  /**
   *
   * @param name The name of the moving average to get the value for
   * @return The current moving average if found
   */
  public long getAverage(String name){
    for(MovingAverage average:movingAverages){
      if(average.getName().equals(name)){
        return average.getAverage();
      }
    }
    return -1;
  }


  /**
   *
   * @return A String representation of this instance
   */
  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder(getClass().getName()+":");
    for(MovingAverage movingAverage:movingAverages){
      sb.append(movingAverage.getName()).append("=").append(movingAverage.getAverage()).append(",");
    }
    return sb.toString();
  }

  /**
   * Resets all the moving averages and clears all data
   */
  public void reset() {
    total.reset();
    dataProcessor.reset();
    for(MovingAverage average:movingAverages){
      average.reset();
    }
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

}
