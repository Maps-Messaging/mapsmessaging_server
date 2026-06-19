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

package io.mapsmessaging.state.stanag.messages.feedback;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Getter
public class TaskFeedbackDetails {

//  @SerializedName("time_remaining")
//  private final String timeRemaining;

  @Singular("waypointRemaining")
  private final List<String> waypointsRemaining;

  @SerializedName("percent_complete")
  private double percentComplete = 0.0d;

  public TaskFeedbackDetails() {
//    timeRemaining = null;
    waypointsRemaining = null;
  }

  public TaskFeedbackDetails(double percentComplete, String timeRemaining, List<String> waypointsRemaining) {
    this.percentComplete = percentComplete;
//    this.timeRemaining = timeRemaining;
    this.waypointsRemaining = waypointsRemaining;
  }
}