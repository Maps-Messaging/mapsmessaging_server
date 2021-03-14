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

package org.maps.selector.operators.comparison;

import org.maps.selector.operators.ComparisonOperator;

public class EqualOperator extends ComparisonOperator {

  public EqualOperator(Object lhs, Object rhs) {
    super(lhs, rhs);
  }

  @Override
  protected Boolean compute(double lhs, double rhs) {
    return (lhs == rhs) || (Double.isNaN(lhs) && Double.isNaN(rhs));
  }

  @Override
  protected Boolean compute(double lhs, long rhs) {
    return lhs == rhs;
  }

  @Override
  protected Boolean compute(long lhs, double rhs) {
    return lhs == rhs;
  }

  @Override
  protected Boolean compute(long lhs, long rhs) {
    return lhs == rhs;
  }

  @Override
  protected Boolean compute(String lhs, String rhs) {
    return lhs.equals(rhs);
  }

  @Override
  protected Boolean compute(Boolean lhs, Boolean rhs){
    return lhs.equals(rhs);
  }

  @Override
  public String toString(){
    return "("+lhs.toString() +") == ("+rhs.toString()+")";
  }

  @Override
  public boolean equals(Object test){
    if(test instanceof EqualOperator){
      return (lhs.equals(((EqualOperator) test).lhs) && rhs.equals(((EqualOperator) test).rhs));
    }
    return false;
  }

  @Override
  public int hashCode(){
    return lhs.hashCode() ^ rhs.hashCode();
  }
}
