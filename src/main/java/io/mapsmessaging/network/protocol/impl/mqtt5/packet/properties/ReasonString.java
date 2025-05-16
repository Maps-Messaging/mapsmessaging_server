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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.types.UTF8MessageProperty;

public class ReasonString extends UTF8MessageProperty {

  public ReasonString(String reason) {
    this();
    value = reason;
  }

  ReasonString() {
    super(MessagePropertyFactory.REASON_STRING, "ReasonString");
  }

  @Override
  public MessageProperty instance() {
    return new ReasonString();
  }

  public String getReasonString() {
    return value;
  }

  public void setReasonString(String reasonString) {
    value = reasonString;
  }
}
