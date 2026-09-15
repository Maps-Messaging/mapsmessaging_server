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

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.mavlink.ProcessedFrame;
import java.util.Map;

final class MavlinkFeedbackTargetFilter {

  private static final int MISSION_REQUEST = 40;
  private static final int MISSION_ACK = 47;
  private static final int MISSION_REQUEST_INT = 51;
  private static final int COMMAND_ACK = 77;

  private MavlinkFeedbackTargetFilter() {
  }

  static boolean allow(ProcessedFrame frame, MavlinkConfigDTO config) {
    if (frame == null || config == null || !config.hasLocalMavlinkIdentity()) {
      return true;
    }

    int messageId = frame.getFrame().getMessageId();
    if (!isFeedbackMessage(messageId)) {
      return true;
    }

    Map<String, Object> fields = frame.getFields();
    return targetMatches(fields.get("target_system"), config.getSystemId())
        && targetMatches(fields.get("target_component"), config.getComponentId());
  }

  private static boolean isFeedbackMessage(int messageId) {
    return messageId == MISSION_REQUEST
        || messageId == MISSION_REQUEST_INT
        || messageId == MISSION_ACK
        || messageId == COMMAND_ACK;
  }

  private static boolean targetMatches(Object value, int localId) {
    if (!(value instanceof Number number)) {
      return true;
    }
    int target = number.intValue();
    return target == 0 || target == localId;
  }
}
