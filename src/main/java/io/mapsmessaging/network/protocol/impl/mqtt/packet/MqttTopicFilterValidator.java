/*
 *
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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

/** Validates MQTT Topic Filters before registering a broker subscription. */
public final class MqttTopicFilterValidator {

  private MqttTopicFilterValidator() {
  }

  public static boolean isShared(String filter) {
    return filter != null && filter.startsWith("$share/");
  }

  public static void validate(String filter) throws MalformedException {
    if (filter == null || filter.isEmpty()) {
      throw new MalformedException("MQTT Topic Filter must not be empty");
    }

    String topic = filter;
    if (isShared(filter)) {
      int separator = filter.indexOf('/', "$share/".length());
      if (separator < 0) {
        throw new MalformedException("Shared subscription needs a group and Topic Filter");
      }
      String group = filter.substring("$share/".length(), separator);
      if (group.isEmpty() || group.indexOf('+') >= 0 || group.indexOf('#') >= 0) {
        throw new MalformedException("Invalid shared subscription group");
      }
      topic = filter.substring(separator + 1);
      if (topic.isEmpty()) {
        throw new MalformedException("Shared subscription Topic Filter must not be empty");
      }
    }

    String[] levels = topic.split("/", -1);
    for (int i = 0; i < levels.length; i++) {
      String level = levels[i];
      if (level.indexOf('+') >= 0 && !level.equals("+")) {
        throw new MalformedException("Single-level wildcard must occupy a whole level");
      }
      if (level.indexOf('#') >= 0 && (!level.equals("#") || i != levels.length - 1)) {
        throw new MalformedException("Multi-level wildcard must be the last whole level");
      }
    }
  }
}
