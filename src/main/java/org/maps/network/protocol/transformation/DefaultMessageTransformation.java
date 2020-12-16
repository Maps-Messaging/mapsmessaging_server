/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.maps.network.protocol.transformation;

import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.message.Message;
import org.maps.network.protocol.ProtocolMessageTransformation;

public class DefaultMessageTransformation implements ProtocolMessageTransformation {

  public DefaultMessageTransformation(){
    // Used by the java services
  }

  @Override
  public String getName() {
    return "Default";
  }

  @Override
  public String getDescription() {
    return "Default Transformation, does nothing to the messages";
  }

  @Override
  public @NotNull Message incoming(@NotNull MessageBuilder messageBuilder) {
    return messageBuilder.build();
  }

  @Override
  public @NotNull byte[] outgoing(@NotNull Message message) {
    return message.getOpaqueData();
  }
}
