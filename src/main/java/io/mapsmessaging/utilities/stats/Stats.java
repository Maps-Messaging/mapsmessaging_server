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

public interface Stats {

  String getName();
  String getUnits();
  void reset();

  long getPerSecond();
  long getCurrent();
  long getTotal();

  void add(long value);
  void subtract(long value);

  default void increment() {
    add(1);
  }
  default void decrement() {
    subtract(1);
  }

  default void update(){}

  default boolean supportMovingAverage(){return false;}

}
