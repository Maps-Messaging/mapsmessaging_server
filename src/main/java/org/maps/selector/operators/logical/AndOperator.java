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

public class AndOperator extends LogicalOperator {

  public AndOperator(Object lhs, Object rhs) {
    super( lhs, rhs);
  }

  @Override
  public Object evaluate(IdentifierResolver resolver) throws ParseException {
    return(test(lhs, resolver) && test(rhs, resolver));
  }

  public Object compile(){
    if(lhs instanceof Operation){
      lhs = ((Operation)lhs).compile();
    }
    if(rhs instanceof Operation){
      rhs = ((Operation)rhs).compile();
    }
    if(lhs instanceof Boolean && rhs instanceof Boolean){
      return ((Boolean)lhs) && ((Boolean)rhs);
    }
    return this;
  }

  @Override
  public String toString(){
    return "("+lhs.toString() +") AND ("+rhs.toString()+")";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof AndOperator){
      return (lhs.equals(((AndOperator) test).lhs) && rhs.equals(((AndOperator) test).rhs));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return lhs.hashCode() ^ rhs.hashCode();
  }
}
