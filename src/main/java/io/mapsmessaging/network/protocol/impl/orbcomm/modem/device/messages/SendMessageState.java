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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages;

public class SendMessageState implements ModemMessage {
  public enum State {
    TX_READY(4),
    TX_SENDING(5),
    TX_COMPLETED(6),
    TX_FAILED(7),
    UNKNOWN(-1);

    private final int code;

    State(int code) {
      this.code = code;
    }

    public int getCode() {
      return code;
    }

    public static State fromCode(int code) {
      return switch (code) {
        case 4 -> TX_READY;
        case 5 -> TX_SENDING;
        case 6 -> TX_COMPLETED;
        case 7 -> TX_FAILED;
        default -> UNKNOWN;
      };
    }
  }

  private final String messageName;
  private final double messageNumber;
  private final int priority;
  private final int sin;
  private final State state;
  private final int length;
  private final int bytesAcknowledged;

  public SendMessageState(String line) {
    if (!line.startsWith("%MGRS:")) {
      throw new IllegalArgumentException("Not a valid %MGRS line: " + line);
    }

    String content = line.substring(line.indexOf(':') + 1).trim();
    String[] parts = content.split(",");

    if (parts.length != 7) {
      throw new IllegalArgumentException("Expected 7 fields, got " + parts.length + ": " + line);
    }

    this.messageName = parts[0].replace("\"", "").trim();
    this.messageNumber = Double.parseDouble(parts[1].trim());
    this.priority = Integer.parseInt(parts[2].trim());
    this.sin = Integer.parseInt(parts[3].trim());
    this.state = State.fromCode(Integer.parseInt(parts[4].trim()));
    this.length = Integer.parseInt(parts[5].trim());
    this.bytesAcknowledged = Integer.parseInt(parts[6].trim());
  }

  public String getMessageName() { return messageName; }
  public double getMessageNumber() { return messageNumber; }
  public int getPriority() { return priority; }
  public int getSin() { return sin; }
  public State getState() { return state; }
  public int getLength() { return length; }
  public int getBytesAcknowledged() { return bytesAcknowledged; }

  public boolean isReady()     { return state == State.TX_READY; }
  public boolean isSending()   { return state == State.TX_SENDING; }
  public boolean isComplete()  { return state == State.TX_COMPLETED; }
  public boolean isFailed()    { return state == State.TX_FAILED; }

  @Override
  public String toString() {
    return String.format("SendMessageState[name=%s, num=%.1f, pri=%d, sin=%d, state=%s, len=%d, ack=%d]",
        messageName, messageNumber, priority, sin, state, length, bytesAcknowledged);
  }
}
