/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.types.ShortMessageProperty;

public class ServerKeepAlive extends ShortMessageProperty {

  ServerKeepAlive() {
    super(MessagePropertyFactory.SERVER_KEEPALIVE, "ServerKeepAlive");
  }

  public ServerKeepAlive(int keepAlive) {
    this();
    value = keepAlive;
  }

  @Override
  public MessageProperty instance() {
    return new ServerKeepAlive();
  }

  public int getServerKeepAlive() {
    return value;
  }

  public void setServerKeepAlive(int serverKeepAlive) {
    value = serverKeepAlive;
  }
}
