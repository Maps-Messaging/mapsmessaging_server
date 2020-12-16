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

package org.maps.messaging.engine.selector.operators.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.message.TypedData;
import org.maps.messaging.engine.selector.Identifier;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.FunctionOperator;
import org.maps.messaging.engine.selector.operators.parsers.ParserFactory;

public class ParserTest {


  @Test
  public void parserLoadTest() throws ParseException {
    String[] arguments = {"value"};
    FunctionOperator operation = ParserFactory.getInstance().loadParser("json", Arrays.asList(arguments));
    String jsonString = "{test:10; value:20}";
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(jsonString.getBytes());
    Assertions.assertEquals(20l, operation.evaluate(messageBuilder.build()));
  }


  @Test
  public void parserPathLoadTest() throws ParseException {
    String[] arguments = {"second.value"};
    FunctionOperator operation = ParserFactory.getInstance().loadParser("json", Arrays.asList(arguments));
    String jsonString = "{test:10; value:20; second:{value:430; test:20 } }";
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(jsonString.getBytes());
    Assertions.assertEquals(430l, operation.evaluate(messageBuilder.build()));
  }

  @Test
  public void parseFromIdentifier() throws ParseException{
    String[] arguments = {"value"};
    FunctionOperator operation = ParserFactory.getInstance().loadParser(new Identifier("protocol"), Arrays.asList(arguments));
    String jsonString = "{test:10; value:20}";
    MessageBuilder messageBuilder = new MessageBuilder();
    Map<String, TypedData> dataMap = new HashMap<>();
    dataMap.put("protocol", new TypedData("json"));
    messageBuilder.setDataMap(dataMap);
    messageBuilder.setOpaqueData(jsonString.getBytes());
    Assertions.assertEquals(20l, operation.evaluate(messageBuilder.build()));
  }
}
