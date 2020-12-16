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

package org.maps.messaging.engine.selector.operators.arithmetic;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.Operation;

public class NegateOperator extends Operation {

  private Object lhs;

  public NegateOperator(Object lhs) {
    this.lhs = lhs;
  }

  @Override
  public Object evaluate(Message message) throws ParseException {
    return compute(evaluate(lhs, message));
  }


  private Object compute(Object result){
    if (result instanceof Double) {
      return -1.0 * ((Double) result);
    }
    else if (result instanceof Float ) {
        return -1.0 * ((Float) result);
    } else if (result instanceof Number) {
      return -1L * ((Number) result).longValue();
    }
    return result;
  }

  @Override
  public Object compile() {
    if(lhs instanceof Operation){
      lhs = ((Operation)lhs).compile();
    }
    // If its a number lets just compute it and return it
    if(lhs instanceof Number){
      return compute(lhs);
    }
    return this;
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof NegateOperator){
      return (lhs.equals(((NegateOperator) test).lhs));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return ~lhs.hashCode();
  }

}
