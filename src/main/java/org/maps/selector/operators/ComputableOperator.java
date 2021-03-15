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


public abstract class ComputableOperator extends Operation {

  protected Object processDouble(Double arg0, Number arg1) {
    if (arg1 instanceof Double || arg1 instanceof Float) {
      return compute(arg0, arg1.doubleValue());
    } else {
      return compute(arg0, arg1.longValue());
    }
  }

  protected Object processInteger(Long arg0, Number arg1) {
    if (arg1 instanceof Double) {
      return compute(arg0, arg1.doubleValue());
    } else {
      return compute(arg0, arg1.longValue());
    }
  }

  public Object compile(Object lhs, Object rhs){
    lhs = compile(lhs);
    rhs = compile(rhs);
    if(lhs instanceof Number && rhs instanceof Number){
      if(lhs instanceof Double){
        return processDouble((Double)lhs, (Number)rhs);
      }
      return processInteger((Long)lhs, (Number) rhs);
    }
    return this;
  }

  protected abstract Object compute(double lhs, double rhs);

  protected abstract Object compute(double lhs, long rhs);

  protected abstract Object compute(long lhs, double rhs);

  protected abstract Object compute(long lhs, long rhs);
}