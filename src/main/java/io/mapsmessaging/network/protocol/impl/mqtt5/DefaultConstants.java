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

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.api.features.Priority;

public class DefaultConstants {


  public static final int SERVER_RECEIVE_MAXIMUM = 2048;
  public static final int CLIENT_RECEIVE_MAXIMUM = 2048;
  public static final int SERVER_TOPIC_ALIAS_MAX = 2048;
  public static final int CLIENT_TOPIC_ALIAS_MAX = 2048;
  public static final int SESSION_TIME_OUT = 86400;
  public static final Priority PRIORITY = Priority.NORMAL;

  public static final int KEEPALIVE_MAXIMUM = 600;
  public static final int KEEPALIVE_MINIMUM = 60;
  public static final int BITSET_BLOCK_SIZE = 4096;

  private DefaultConstants() {
  }
}
