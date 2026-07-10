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

package io.mapsmessaging.state.mavlink.sender;

import io.mapsmessaging.state.mavlink.messages.MavlinkMessage;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;

public interface MavlinkAcknowledgementHandler {

  boolean requiresAcknowledgement(MavlinkMessage sentMessage);

  Acknowledgement acknowledge(MavlinkMessage sentMessage, MavlinkPacket receivedMessage);

  record Acknowledgement(Action action, int index, String reason) {

    public Acknowledgement {
      action = action == null ? Action.NOT_RELATED : action;
    }

    public static Acknowledgement notRelated() {
      return new Acknowledgement(Action.NOT_RELATED, -1, null);
    }

    public static Acknowledgement waitForMore() {
      return new Acknowledgement(Action.WAIT, -1, null);
    }

    public static Acknowledgement advance() {
      return new Acknowledgement(Action.ADVANCE, -1, null);
    }

    public static Acknowledgement sendIndex(int index) {
      return new Acknowledgement(Action.SEND_INDEX, index, null);
    }

    public static Acknowledgement complete() {
      return new Acknowledgement(Action.COMPLETE, -1, null);
    }

    public static Acknowledgement fail(String reason) {
      return new Acknowledgement(Action.FAIL, -1, reason);
    }
  }

  enum Action {
    NOT_RELATED,
    WAIT,
    ADVANCE,
    SEND_INDEX,
    COMPLETE,
    FAIL
  }
}