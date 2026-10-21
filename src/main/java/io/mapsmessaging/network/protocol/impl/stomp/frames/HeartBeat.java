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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;

public class HeartBeat {

  private final int canSend;
  private final int wantsReceive;

  HeartBeat(String heartBeat) throws StompProtocolException {
    if (heartBeat == null) {
      throw new StompProtocolException("Missing STOMP heart-beat value");
    }
    String[] values = heartBeat.split(",", -1);
    if (values.length != 2) {
      throw new StompProtocolException("STOMP heart-beat must contain two values");
    }
    canSend = parseValue(values[0]);
    wantsReceive = parseValue(values[1]);
  }

  public HeartBeat(int canSend, int wantsReceive) {
    if (canSend < 0 || wantsReceive < 0) {
      throw new IllegalArgumentException("STOMP heartbeat values must not be negative");
    }
    this.canSend = canSend;
    this.wantsReceive = wantsReceive;
  }

  @Override
  public String toString() {
    return canSend + "," + wantsReceive;
  }

  public int getCanSend() {
    return canSend;
  }

  public int getWantsReceive() {
    return wantsReceive;
  }

  public int getMinimum() {
    return canSend;
  }

  public int getPreferred() {
    return wantsReceive;
  }

  public static long negotiate(int senderCanSend, int receiverWantsReceive) {
    if (senderCanSend == 0 || receiverWantsReceive == 0) {
      return 0;
    }
    return Math.max(senderCanSend, receiverWantsReceive);
  }

  private int parseValue(String value) throws StompProtocolException {
    try {
      int parsed = Integer.parseInt(value.trim());
      if (parsed < 0) {
        throw new StompProtocolException("STOMP heartbeat values must not be negative");
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw new StompProtocolException("Invalid STOMP heartbeat value " + value);
    }
  }
}
