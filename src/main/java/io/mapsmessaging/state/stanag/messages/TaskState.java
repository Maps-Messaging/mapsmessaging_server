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

package io.mapsmessaging.state.stanag.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskState {

  PENDING(false),
  ACTIVE(false),
  RECALLING(false),
  PREEMPTING(false),
  PAUSING(false),
  PAUSED(false),
  RESUMING(false),
  ON_HOLD(false),
  ACTIONABLE(false),
  PREPARED(false),
  PLANNING(false),
  WAITING_FOR_PUSH_ACK(false),
  WAITING_FOR_CANCEL_ACK(false),
  WAITING_FOR_PAUSE_ACK(false),
  WAITING_FOR_RESUME_ACK(false),

  REJECTED(true),
  RECALLED(true),
  PREEMPTED(true),
  ABORTED(true),
  SUCCEEDED(true),
  LOST(true);

  private final boolean terminal;
}