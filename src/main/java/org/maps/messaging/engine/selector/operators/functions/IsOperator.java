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

package org.maps.messaging.engine.selector.operators.functions;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.FunctionOperator;

public class IsOperator extends FunctionOperator {

  private final Object lhs;
  private final boolean not;

  public IsOperator(Object lhs, boolean not){
    this.lhs = lhs;
    this.not = not;
  }

  public Object compile(){
    return this;
  }

  @Override
  public Object evaluate(Message message) throws ParseException {
    Object lookup = evaluate(lhs, message);
    boolean isNull = lookup == null;
    if(not){
      return !isNull;
    }
    return isNull;
  }

  public String toString(){
    if(not) {
      return "(" + lhs.toString() + ") IS NOT NULL";
    }
    else{
      return "(" + lhs.toString() + ") IS NULL";
    }
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof IsOperator){
      return (lhs.equals(((IsOperator) test).lhs) && not == ((IsOperator) test).not);
    }
    return false;
  }

  @Override
  public int hashCode(){
    if(not) {
      return ~lhs.hashCode();
    }
    return lhs.hashCode();
  }

}