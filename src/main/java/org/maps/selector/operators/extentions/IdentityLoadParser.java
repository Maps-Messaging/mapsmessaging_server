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

package org.maps.selector.operators.extentions;

import java.util.List;
import org.maps.selector.Identifier;
import org.maps.selector.ParseException;
import org.maps.selector.operators.FunctionOperator;
import org.maps.selector.operators.IdentifierResolver;

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
  public Object evaluate(IdentifierResolver resolver) throws ParseException {
    Object parserName = evaluate(parserIdentifier, resolver);
    if(parserName != null){
      FunctionOperator parser = ParserFactory.getInstance().loadParser(parserName, arguments);
      if(parser != null){
        return convertResult(parser.evaluate(resolver));
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
