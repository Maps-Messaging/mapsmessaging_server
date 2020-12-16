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

package org.maps.network.protocol.impl.amqp.proton.transformers;

import org.maps.network.protocol.impl.amqp.proton.transformers.impl.BaseMessageTranslator;
import org.maps.network.protocol.impl.amqp.proton.transformers.impl.ByteMessageTranslator;
import org.maps.network.protocol.impl.amqp.proton.transformers.impl.MapMessageTranslator;
import org.maps.network.protocol.impl.amqp.proton.transformers.impl.ObjectMessageTranslator;
import org.maps.network.protocol.impl.amqp.proton.transformers.impl.StreamMessageTranslator;
import org.maps.network.protocol.impl.amqp.proton.transformers.impl.TextMessageTranslator;

public enum MessageTypes {

  MESSAGE(0, "Generic JMS Message", new BaseMessageTranslator()),
  OBJECT(1, "Object Message", new ObjectMessageTranslator()),
  MAP(2, "Map Message", new MapMessageTranslator()),
  BYTES(3, "Byte Message", new ByteMessageTranslator()),
  STREAM(4, "Stream Message", new StreamMessageTranslator()),
  TEXT(5, "Text Message", new TextMessageTranslator());


  public static MessageTypes getInstance(int value){
    switch (value){
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

  MessageTypes(int value, String description, MessageTranslator messageTranslator){
    this.value = value;
    this.description = description;
    this.messageTranslator = messageTranslator;
  }

  public int getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }

  public MessageTranslator getMessageTranslator(){
    return messageTranslator;
  }
}
