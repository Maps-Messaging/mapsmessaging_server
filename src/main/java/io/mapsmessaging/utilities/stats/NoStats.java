/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

import lombok.Getter;

public class NoStats implements Stats{

  @Getter
  private final String name;
  @Getter
  private final String units;


  public NoStats(String name, String unitName) {
    this.name = name;
    this.units = unitName;
  }

  @Override
  public void reset() {

  }

  @Override
  public float getPerSecond() {
    return 0f;
  }

  @Override
  public long getCurrent() {
    return 0;
  }

  @Override
  public long getTotal() {
    return 0;
  }

  @Override
  public void add(long value) {

  }

  @Override
  public void subtract(long value) {

  }
}
