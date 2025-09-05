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

package io.mapsmessaging.api.features;

import lombok.Getter;

@Getter
public enum RollbackPriority {
  MAINTAIN("maintain", "Maintain Priority", 0),
  INCREMENT("increment", "Increment Priority", 1);


  private final String name;
  private final int priorityDelta;
  private final String description;

  RollbackPriority(String name, String description, int priorityDelta) {
    this.name = name;
    this.priorityDelta = priorityDelta;
    this.description = description;
  }

  public int incrementPriority(int originalPriority) {
    int val = priorityDelta + originalPriority;
    if(val> Priority.HIGHEST.getValue()){
      val = Priority.HIGHEST.getValue();
    }
    return val;
  }
}
