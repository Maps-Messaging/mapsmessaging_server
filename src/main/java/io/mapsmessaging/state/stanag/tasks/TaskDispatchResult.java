/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.stanag.tasks;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.stanag.messages.result.ResultReason;
import io.mapsmessaging.state.stanag.tasks.monitor.TaskMonitor;
import lombok.Getter;

@Getter
public class TaskDispatchResult {

  private final DroneTwin droneTwin;
  private final TaskMonitor taskMonitor;
  private final ResultReason rejectReason;
  private final String rejectText;

  private TaskDispatchResult(DroneTwin droneTwin, TaskMonitor taskMonitor, ResultReason rejectReason, String rejectText) {
    this.droneTwin = droneTwin;
    this.taskMonitor = taskMonitor;
    this.rejectReason = rejectReason;
    this.rejectText = rejectText;
  }

  public static TaskDispatchResult accepted(DroneTwin droneTwin, TaskMonitor taskMonitor) {
    return new TaskDispatchResult(droneTwin, taskMonitor, null, null);
  }

  public static TaskDispatchResult rejected(DroneTwin droneTwin, ResultReason rejectReason, String rejectText) {
    return new TaskDispatchResult(droneTwin, null, rejectReason, rejectText);
  }

  public boolean isAccepted() {
    return rejectReason == null;
  }
}