/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
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
 *
 */

package io.mapsmessaging.engine.system.impl.load;

import io.mapsmessaging.engine.system.SystemTopicWithAverage;
import io.mapsmessaging.network.io.EndPoint;

public class Received extends SystemTopicWithAverage {

  public Received() {
    super("$SYS/broker/load/bytes/received", true);
  }

  @Override
  public String[] aliases() {
    return new String[]{"$SYS/broker/bytes/received", "$SYS/bytes/received"};
  }

  @Override
  public long getData() {
    return EndPoint.totalReadBytes.sum();
  }
}
