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

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers;

import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.*;
import lombok.Getter;

@Getter
public enum MessageTypes {

  MESSAGE(0, "Generic JMS Message", new BaseMessageTranslator()),
  OBJECT(1, "Object Message", new ObjectMessageTranslator()),
  MAP(2, "Map Message", new MapMessageTranslator()),
  BYTES(3, "Byte Message", new ByteMessageTranslator()),
  STREAM(4, "Stream Message", new StreamMessageTranslator()),
  TEXT(5, "Text Message", new TextMessageTranslator());


  public static MessageTypes getInstance(int value) {
    switch (value) {
      case 1:
        return OBJECT;
      case 2:
        return MAP;
      case 3:
        return BYTES;
      case 4:
        return STREAM;
      case 5:
        return TEXT;
      case 0:
      default:
        return MESSAGE;
    }
  }

  private final int value;
  private final String description;
  private final MessageTranslator messageTranslator;

  MessageTypes(int value, String description, MessageTranslator messageTranslator) {
    this.value = value;
    this.description = description;
    this.messageTranslator = messageTranslator;
  }
}