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

package io.mapsmessaging.utilities.stats;

import lombok.Getter;

import java.util.Map;

public class LinkedMovingAverageRecord {

  @Getter
  private final String name;
  @Getter
  private final String unitName;
  @Getter
  private final long timeSpan;
  @Getter
  private final long current;

  @Getter
  private final Map<String, Long> stats;

  public LinkedMovingAverageRecord(String name, String unitName, long timeSpan, long current, Map<String, Long> stats ){
    this.name = name;
    this.unitName = unitName;
    this.timeSpan = timeSpan;
    this.current = current;
    this.stats = stats;
  }

}
