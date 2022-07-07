/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.detection.Detection;

public class NMEAProtocolDetection implements Detection {

  @Override
  public boolean detected(Packet packet) throws EndOfBufferException {
    int pos = packet.position();
    try {
      NMEAPacket nmeaPacket = new NMEAPacket(packet);
      if (nmeaPacket.getSentence().length() == 0) {
        throw new EndOfBufferException("No NMEA packet detected");
      }
      return true;
    } catch (EndOfBufferException tryAgain) {
      throw tryAgain;
    } catch (Exception e) {
      return false;
    } finally {
      packet.position(pos); // roll it back
    }
  }

  @Override
  public int getHeaderSize() {
    return 20;
  }

}
