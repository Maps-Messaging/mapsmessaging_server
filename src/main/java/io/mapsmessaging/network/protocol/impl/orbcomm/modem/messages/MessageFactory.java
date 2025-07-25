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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.messages;

public class MessageFactory {

  public static ModemMessage create(int min, byte[] data){
    switch(min){
      case 0:
        return new ModemIdentificationMessage(data);
      case 1:
        return new ProtocolErrorMessage(data);
      case 70:
        return new SleepScheduleMessage(data);
      case 72:
        return new PositionMessage(data);
      case 97:
        return new PingResponseMessage(data);
      case 113:
        return new PingRequestMessage(data);
      case 115:
        return new BroadcastIdMessage(data);
      default:
        return null;
    }
  }

  private MessageFactory() {}
}
