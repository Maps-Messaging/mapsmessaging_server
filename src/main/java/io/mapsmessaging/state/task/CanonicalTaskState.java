/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.task;

public enum CanonicalTaskState {
  PENDING,
  ACTIVE,
  COMPLETED,
  REJECTED,
  ABORTED,
  PREEMPTING,
  PREEMPTED,
  LOST;

  public boolean isTerminal() {
    return this == COMPLETED || this == REJECTED || this == ABORTED || this == PREEMPTED || this == LOST;
  }

  public boolean canTransitionTo(CanonicalTaskState candidate) {
    if (candidate == null || candidate == this || isTerminal()) {
      return false;
    }
    return switch (this) {
      case PENDING -> true;
      case ACTIVE -> candidate != PENDING && candidate != REJECTED;
      case PREEMPTING -> candidate != PENDING && candidate != ACTIVE && candidate != REJECTED;
      case COMPLETED, REJECTED, ABORTED, PREEMPTED, LOST -> false;
    };
  }
}
