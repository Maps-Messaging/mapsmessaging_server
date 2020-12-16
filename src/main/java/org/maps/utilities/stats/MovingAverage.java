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

public class MovingAverage  {

  private final List<DataPoint> dataPoints;
  private final long timePeriod;
  private final String name;
  private final int expectedEntries;

  public MovingAverage(int time, TimeUnit unit){
    this.name = time+"_"+unit.toString();
    timePeriod = unit.toMillis(time);
    dataPoints = new ArrayList<>();
    expectedEntries = time;
  }

  public String getName(){
    return name;
  }

  public void add(long data){
    long now = System.currentTimeMillis();
    dataPoints.add(new DataPoint(data, now +  + timePeriod));
    clearData(now);
  }

  public long getAverage(){
    clearData(System.currentTimeMillis());
    long ave = 0;
    for(DataPoint dataPoint:dataPoints){
      ave += dataPoint.data;
    }
    return ave/expectedEntries;
  }

  protected void clearData(long now){
    while(!dataPoints.isEmpty() && dataPoints.get(0).expiry < now ){
      dataPoints.remove(0);
    }
  }

  public void reset() {
    dataPoints.clear();
  }

  protected static class DataPoint {
    protected final long data;
    protected final long expiry;

    public DataPoint(long data, long delay){
      this.data = data;
      expiry = delay;
    }
  }

}
