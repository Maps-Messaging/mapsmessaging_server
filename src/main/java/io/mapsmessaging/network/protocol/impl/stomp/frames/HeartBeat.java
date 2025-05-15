/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import java.util.StringTokenizer;

public class HeartBeat {

  private final int minimum;
  private final int preferred;

  HeartBeat(String heartBeat) {
    StringTokenizer st = new StringTokenizer(heartBeat, ",");
    minimum = Integer.parseInt(st.nextElement().toString());
    preferred = Integer.parseInt(st.nextElement().toString());
  }

  public HeartBeat(int minimum, int preferred) {
    this.minimum = minimum;
    this.preferred = preferred;
  }

  @Override
  public String toString() {
    return minimum + "," + preferred;
  }

  public int getMinimum() {
    return minimum;
  }

  public int getPreferred() {
    return preferred;
  }
}
