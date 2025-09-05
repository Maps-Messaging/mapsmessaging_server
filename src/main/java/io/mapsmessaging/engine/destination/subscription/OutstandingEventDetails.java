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

package io.mapsmessaging.engine.destination.subscription;

public class OutstandingEventDetails {

  private final long id;
  private final int priority;
  private final long time;

  public OutstandingEventDetails(long id, int priority) {
    this.id = id;
    this.priority = priority;
    time = System.currentTimeMillis();
  }

  public long getId() {
    return id;
  }

  public int getPriority() {
    return priority;
  }

  public long getTime() {
    return time;
  }

  public String toString() {
    return "ID:"
        + id
        + " Priority:"
        + priority
        + " Age:"
        + (System.currentTimeMillis() - time)
        + "ms";
  }
}
