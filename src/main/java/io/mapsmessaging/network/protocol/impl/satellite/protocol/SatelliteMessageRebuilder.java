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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SatelliteMessageRebuilder {

  private final Map<Integer, List<SatelliteMessage>> fragments;


  public SatelliteMessageRebuilder() {
    fragments = new HashMap<>();
  }

  public void clear() {
    fragments.clear();
  }

  public SatelliteMessage rebuild(SatelliteMessage message) {
    if(!fragments.containsKey(message.getStreamNumber()) && !message.isCompressed() && message.getPacketNumber() == 0) {
      return message;
    }

    fragments
        .computeIfAbsent(message.getStreamNumber(), k -> new ArrayList<>())
        .add(message);

    if (message.getPacketNumber() == 0) {
      List<SatelliteMessage> list = fragments.remove(message.getStreamNumber());
      if (list != null) {
        return SatelliteMessageFactory.reconstructMessage(list);
      }
    }
    return null;
  }

}
