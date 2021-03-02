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

package org.maps.messaging.engine.selector.operators.parsers;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.FunctionOperator;

public class ParserProxy extends FunctionOperator {

  private final SelectorParser parser;

  public ParserProxy(SelectorParser parser){
    this.parser = parser;
  }

  public Object compile(){
    return this;
  }

  @Override
  public Object evaluate(Message message) throws ParseException {
    Object result = parser.parse(message);
    if(result instanceof Number || result instanceof String || result instanceof Boolean){
      if(result instanceof Double){
        return result;
      }
      if(result instanceof Float){
        return ((Float)result).doubleValue();
      }
      else if(result instanceof Number){
        return ((Number)result).longValue(); // Forces byte, short, int all to long
      }
      return result;
    }
    return false;
  }

  @Override
  public String toString(){
    return "Parse ("+parser.toString()+ ")";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof ParserProxy){
      return parser.equals(((ParserProxy) test).parser);
    }
    return false;
  }

  @Override
  public int hashCode(){
    return parser.hashCode();
  }

}
