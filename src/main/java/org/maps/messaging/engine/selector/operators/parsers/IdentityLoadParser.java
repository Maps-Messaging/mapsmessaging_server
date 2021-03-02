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

import java.util.List;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.Identifier;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.FunctionOperator;

public class IdentityLoadParser extends FunctionOperator {

  private final Identifier parserIdentifier;
  private final List<String> arguments;

  public IdentityLoadParser(Identifier parser, List<String> arguments){
    this.parserIdentifier = parser;
    this.arguments = arguments;
  }

  public Object compile(){
    return this;
  }

  @Override
  public Object evaluate(Message message) throws ParseException {
    Object parserName = evaluate(parserIdentifier, message);
    if(parserName != null){
      FunctionOperator parser = ParserFactory.getInstance().loadParser(parserName, arguments);
      if(parser != null){
        Object result = parser.evaluate(message);
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
    }
    return false;
  }

  @Override
  public String toString(){
    StringBuilder tmp = new StringBuilder("Parse (" + parserIdentifier + ", ");
    for(String check:arguments)
      tmp.append(check).append(",");

    tmp.append(")");
    return tmp.toString();
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof IdentityLoadParser){
      return (parserIdentifier.equals(((IdentityLoadParser) test).parserIdentifier) && arguments.equals(((IdentityLoadParser) test).arguments));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return parserIdentifier.hashCode() | arguments.hashCode();
  }

}
