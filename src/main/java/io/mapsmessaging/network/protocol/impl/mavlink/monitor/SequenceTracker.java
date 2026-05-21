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

package io.mapsmessaging.network.protocol.impl.mavlink.monitor;

public class SequenceTracker {

  private static final int RESET_THRESHOLD = 32;

  private Integer lastSequenceNumber;

  private SequenceStatus lastStatus;

  public SequenceResult accept(int currentSequenceNumber) {

    if (currentSequenceNumber < 0 || currentSequenceNumber > 255) {
      throw new IllegalArgumentException("Sequence number must be between 0 and 255");
    }

    if (lastSequenceNumber == null) {

      SequenceResult result = SequenceResult.builder()
          .previousSequenceNumber(-1)
          .currentSequenceNumber(currentSequenceNumber)
          .expectedSequenceNumber(currentSequenceNumber)
          .delta(0)
          .lostPackets(0)
          .statusChanged(true)
          .timestamp(System.currentTimeMillis())
          .status(SequenceStatus.INITIAL)
          .build();

      lastSequenceNumber = currentSequenceNumber;
      lastStatus = SequenceStatus.INITIAL;
      return result;
    }

    int previousSequenceNumber = lastSequenceNumber;

    int expectedSequenceNumber = (previousSequenceNumber + 1) & 0xFF;

    int delta = (currentSequenceNumber - previousSequenceNumber) & 0xFF;

    SequenceStatus status;
    int lostPackets = 0;

    if (delta == 1) {
      status = SequenceStatus.OK;
    }
    else if (delta > 1 && delta <= RESET_THRESHOLD) {
      status = SequenceStatus.LOSS;
      lostPackets = delta - 1;
    }
    else if (delta > RESET_THRESHOLD) {
      status = SequenceStatus.RESET;
    }
    else {
      status = SequenceStatus.OUT_OF_ORDER;
    }

    boolean statusChanged = status != lastStatus;

    SequenceResult result = SequenceResult.builder()
        .previousSequenceNumber(previousSequenceNumber)
        .currentSequenceNumber(currentSequenceNumber)
        .expectedSequenceNumber(expectedSequenceNumber)
        .delta(delta)
        .lostPackets(lostPackets)
        .statusChanged(statusChanged)
        .timestamp(System.currentTimeMillis())
        .status(status)
        .build();

    lastSequenceNumber = currentSequenceNumber;
    lastStatus = status;

    return result;
  }
}