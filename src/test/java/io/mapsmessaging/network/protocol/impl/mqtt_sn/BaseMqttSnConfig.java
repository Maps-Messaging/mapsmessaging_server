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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.mapsmessaging.test.BaseTestConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class BaseMqttSnConfig extends BaseTestConfig {

  public static final int[] QOS_LIST = {0, 1, 2};
  public static final int[] VERSIONS = {1, 2};

  public static Stream<Arguments> createQoSVersionStream() {
    List<Arguments> args = new ArrayList<>();
    for (int qos : QOS_LIST) {
      for (int verion : VERSIONS) {
        args.add(arguments(qos, verion));
      }
    }
    return args.stream();
  }

  public static Stream<Arguments> createVersionStream() {
    List<Arguments> args = new ArrayList<>();
    for (int verion : VERSIONS) {
      args.add(arguments(verion));
    }
    return args.stream();
  }

}
