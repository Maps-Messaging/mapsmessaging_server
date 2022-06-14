/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.utilities.stats.processors;

import java.util.concurrent.atomic.LongAdder;

public class AverageDataProcessor implements DataProcessor {

  private final LongAdder value;
  private final LongAdder count;

  public AverageDataProcessor(){
    value = new LongAdder();
    count = new LongAdder();
  }

  @Override
  public long add(long current, long previous) {
    value.add(current);
    count.increment();
    return current - previous;
  }

  @Override
  public long calculate() {
    long average = value.sum();
    long counter = count.sum();
    if(counter != 0) {
      average = average / counter;
    }
    else{
      average = 0;
    }
    reset();
    return average;
  }

  @Override
  public void reset() {
    value.reset();
    count.reset();
  }
}
