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

package org.maps.selector.operators;

import org.maps.selector.ParseException;

public abstract class Operation {

  public abstract Object evaluate(IdentifierResolver resolver) throws ParseException;

  public abstract Object compile();

  protected Object compile(Object test){
    if(test instanceof Operation){
      return ((Operation)test).compile();
    }
    return test;
  }

  protected static Object evaluate(Object parameter, IdentifierResolver resolver) throws ParseException {
    if(parameter instanceof Operation){
      return ((Operation) parameter).evaluate(resolver);
    }
    else{
      return parameter;
    }
  }

  protected static Number evaluateToNumber(Object parameter, IdentifierResolver resolver) throws ParseException {
    Object result = evaluate(parameter, resolver);
    if (result instanceof Number) {
      return (Number) result;
    }
    else if(result instanceof String){
      return parseStringToNumber((String)result);
    }
    else if(result == null){
      return null;
    }
    return Double.NaN;
  }

  protected static Number parseStringToNumber(String value){
    try {
      if(value.contains(".")){
        return Double.parseDouble(value);
      }
      else{
        return Long.parseLong(value);
      }
    } catch (NumberFormatException e) {
      // Ignore
      return null;
    }
  }
}
