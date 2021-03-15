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

/**
 * From the JMS 2.0 Specification
 *<p>
 * String and Boolean comparison is restricted to = and &lt;&gt;. Two strings are equal if and only if they contain the same sequence of characters.
 */
public abstract class ComparisonOperator extends ComputableOperator {

  protected Object lhs;
  protected Object rhs;

  protected ComparisonOperator(Object lhs, Object rhs)  {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public Object getRHS(){
    return rhs;
  }

  public Object compile(){
    if(lhs instanceof Operation){
      lhs = ((Operation)lhs).compile();
    }
    if(rhs instanceof Operation){
      rhs = ((Operation)rhs).compile();
    }
    if( (lhs instanceof Number && rhs instanceof Number) ||
        (lhs instanceof Boolean && rhs instanceof Boolean) ||
        (lhs instanceof String && rhs instanceof String)){
      return evaluate(lhs, rhs);
    }
    return this;
  }

  public Object evaluate(IdentifierResolver resolver) throws ParseException {
    Object lhsValue = evaluate(lhs, resolver);
    Object rhsValue = evaluate(rhs, resolver);
    return evaluate(lhsValue, rhsValue);
  }

  public Object evaluate(Object lhsValue, Object rhsValue){
    if(lhsValue instanceof String && rhsValue instanceof String){
      return compute((String) lhsValue, (String)rhsValue);
    }

    if(lhsValue instanceof Boolean && rhsValue instanceof Boolean){
      return compute((Boolean)lhsValue, (Boolean) rhsValue);
    }

    if(lhsValue instanceof Number && rhsValue instanceof Number){
      return processNumber((Number)lhsValue,  (Number)rhsValue);
    }

    if(lhsValue instanceof String && rhsValue != null){
      Number lhsNumber = parseStringToNumber((String)lhsValue);
      if(lhsNumber != null) {
        return evaluate(lhsNumber, rhsValue);
      }
    }
    else if(rhsValue instanceof String && lhsValue != null){
      Number rhsNumber = parseStringToNumber((String)rhsValue);
      if(rhsNumber != null) {
        return evaluate(lhsValue, rhsNumber);
      }
    }
    return false;
  }

  private Object processNumber(Number lhsNumber, Number rhsNumber){
    if (lhsNumber instanceof Double) {
      return processDouble((Double)lhsNumber, rhsNumber);
    } else {
      return processInteger((Long)lhsNumber, rhsNumber);
    }
  }

  // Regardless of the arguments we can not compare strings by default
  @java.lang.SuppressWarnings("squid:S1172")
  protected Boolean compute(String lhs, String rhs) {
    return false;
  }

  // Regardless of the arguments we can not compare boolean by default
  @java.lang.SuppressWarnings("squid:S1172")
  protected Boolean compute(Boolean lhs, Boolean rhs){
    return false;
  }

}