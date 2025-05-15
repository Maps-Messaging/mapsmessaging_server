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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.types.ShortMessageProperty;

public class TopicAlias extends ShortMessageProperty {

  TopicAlias() {
    super(MessagePropertyFactory.TOPIC_ALIAS, "TopicAlias");
  }

  public TopicAlias(int alias) {
    super(MessagePropertyFactory.TOPIC_ALIAS, "TopicAlias");
    setTopicAlias(alias);
  }

  @Override
  public MessageProperty instance() {
    return new TopicAlias();
  }

  public int getTopicAlias() {
    return value;
  }

  public void setTopicAlias(int value) {
    this.value = value;
  }
}
