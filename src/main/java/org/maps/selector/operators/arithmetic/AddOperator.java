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

package org.maps.selector.operators.arithmetic;

import org.maps.selector.ParseException;
import org.maps.selector.operators.ArithmeticOperator;

public class AddOperator extends ArithmeticOperator {

  public AddOperator(Object lhs, Object rhs) throws ParseException {
    super(lhs, rhs);
  }

  @Override
  protected Number compute(double lhs, double rhs) {
    return lhs + rhs;
  }

  @Override
  protected Number compute(double lhs, long rhs) {
    return lhs + rhs;
  }

  @Override
  protected Number compute(long lhs, double rhs) {
    return lhs + rhs;
  }

  @Override
  protected Number compute(long lhs, long rhs) {
    return lhs + rhs;
  }

  @Override
  public String toString() {
    return "(" + lhs.toString() + ") + (" + rhs.toString() + ")";
  }


  @Override
  public boolean equals(Object test) {
    if (test instanceof AddOperator) {
      return (lhs.equals(((AddOperator) test).lhs) && rhs.equals(((AddOperator) test).rhs));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return lhs.hashCode() << 1 ^ rhs.hashCode() >> 1;
  }
}
