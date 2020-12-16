/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.messaging.engine.system.impl.messages.publish;

import org.maps.messaging.engine.system.SystemTopic;

public class Dropped extends SystemTopic {

  public Dropped() {
    super("$SYS/broker/publish/messages/dropped");
  }

  @Override
  public String[] aliases() {
    return new String[]{
        "$SYS/broker/messages/publish/dropped",
        "$SYS/messages/publish/dropped",
        "$SYS/load/publish/dropped"
    };
  }
}
