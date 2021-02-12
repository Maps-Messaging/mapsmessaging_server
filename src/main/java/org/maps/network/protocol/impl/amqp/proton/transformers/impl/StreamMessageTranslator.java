/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.amqp.proton.transformers.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.message.TypedData;
import org.maps.messaging.api.message.TypedData.TYPE;
import org.maps.network.protocol.impl.amqp.proton.transformers.MessageTypes;

public class StreamMessageTranslator extends BaseMessageTranslator {

  @Override
  public @NotNull MessageBuilder decode(@NotNull MessageBuilder messageBuilder, @NotNull org.apache.qpid.proton.message.Message protonMessage){
    super.decode(messageBuilder, protonMessage);
    Section body = protonMessage.getBody();
    if(body instanceof AmqpSequence){
      AmqpSequence sequence = (AmqpSequence)body;
      List list = sequence.getValue();
      Map<String, TypedData> dataMap = messageBuilder.getDataMap();
      for(int x =0;x<list.size();x++){
        Object val = list.get(x);
        if(val instanceof Binary){
          Binary binary = (Binary)val;
          dataMap.put(""+x, new TypedData(binary.getArray()));
        }
        else {
          dataMap.put(""+x, new TypedData(list.get(x)));
        }
      }
    }
    return messageBuilder;
  }

  @Override
  public @NotNull Message encode(@NotNull org.maps.messaging.api.message.Message message) {
    Message protonMessage = super.encode(message);

    int x=0;
    Map<String, TypedData> dataMap = message.getDataMap();
    List<Object> list = new ArrayList<>();
    while(dataMap.containsKey(""+x)){
      TypedData data = dataMap.get(""+x);
      if(data.getType().equals(TYPE.BYTE_ARRAY)){
        list.add(new Binary((byte[]) data.getData()));
      }
      else {
        list.add(data.getData());
      }
      x++;
    }
    AmqpSequence sequence = new AmqpSequence(list);
    protonMessage.setBody(sequence);
    return protonMessage;
  }

  @Override
  protected byte getType(){
    return (byte) MessageTypes.STREAM.getValue();
  }

}
