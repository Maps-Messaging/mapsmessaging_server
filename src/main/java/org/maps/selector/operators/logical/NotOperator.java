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

package org.maps.selector.operators.logical;

import org.maps.selector.ParseException;
import org.maps.selector.operators.IdentifierResolver;
import org.maps.selector.operators.LogicalOperator;
import org.maps.selector.operators.Operation;

public class NotOperator extends LogicalOperator {

  public NotOperator(Object lhs) {
    super( lhs, null);
  }

  @Override
  public Object evaluate(IdentifierResolver resolver) throws ParseException {
    return !test(lhs, resolver);
  }

  public Object compile(){
    if(lhs instanceof Operation){
      lhs = ((Operation)lhs).compile();
    }
    if(lhs instanceof Boolean){
      return !((Boolean)lhs);
    }
    return this;
  }

  @Override
  public String toString(){
    return "NOT ("+lhs.toString() +")";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof NotOperator){
      return (lhs.equals(((NotOperator) test).lhs));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return ~lhs.hashCode();
  }

}
