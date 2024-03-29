/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.utilities.stats.processors;

import java.util.concurrent.atomic.LongAdder;

public class AdderDataProcessor implements DataProcessor {

  private final LongAdder adder;

  public AdderDataProcessor() {
    adder = new LongAdder();
  }

  @Override
  public long calculate() {
    return adder.sumThenReset();
  }

  @Override
  public long add(long current, long previous) {
    adder.add(current);
    return current;
  }

  @Override
  public void reset() {
    adder.reset();
  }

}
