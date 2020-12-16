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

package org.maps.network.protocol.impl.mqtt5.packet.properties;

import org.maps.network.protocol.impl.mqtt5.packet.properties.types.BooleanMessageProperty;

public class SubscriptionIdentifiersAvailable extends BooleanMessageProperty {

  public SubscriptionIdentifiersAvailable(boolean flag) {
    this();
    value = flag;
  }

  SubscriptionIdentifiersAvailable() {
    super(MessagePropertyFactory.SUBSCRIPTION_IDENTIFIERS_AVAILABLE, "SubscriptionIdentifiersAvailable");
  }

  @Override
  public MessageProperty instance() {
    return new SubscriptionIdentifiersAvailable();
  }

  public boolean getSubscriptionIdentifiersAvailable() {
    return value;
  }

  public void setSubscriptionIdentifiersAvailable(boolean available) {
    value = available;
  }
}
