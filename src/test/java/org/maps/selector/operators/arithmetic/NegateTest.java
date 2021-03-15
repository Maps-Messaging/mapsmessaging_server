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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.selector.ParseException;

public class NegateTest {
  @Test
  public void simpleMathTests() throws ParseException {
    NegateOperator negateOperator = new NegateOperator(8L);
    Assertions.assertEquals(-8L, negateOperator.evaluate(null));

    negateOperator = new NegateOperator(8);
    Assertions.assertEquals(-8L, negateOperator.evaluate(null));

    negateOperator = new NegateOperator((short)8);
    Assertions.assertEquals(-8L, negateOperator.evaluate(null));

    negateOperator = new NegateOperator((byte)8);
    Assertions.assertEquals(-8L, negateOperator.evaluate(null));

    negateOperator = new NegateOperator(4.8);
    Assertions.assertEquals(-4.8, negateOperator.evaluate(null));

    negateOperator = new NegateOperator(4.8f);
    Assertions.assertEquals(-4.8, (Math.round((Double)negateOperator.evaluate(null) *10.0) /10.0));

    negateOperator = new NegateOperator( 0L);
    Assertions.assertEquals(0L, negateOperator.evaluate(null));

    negateOperator = new NegateOperator(4.8);
    Assertions.assertEquals(-4.8, negateOperator.compile());


    negateOperator = new NegateOperator(4.8);
    NegateOperator negateOperator2 = new NegateOperator(4.8);
    Assertions.assertEquals(negateOperator, negateOperator2);
    Assertions.assertEquals(negateOperator.hashCode(), negateOperator2.hashCode());

    negateOperator2 = new NegateOperator(10.8);
    Assertions.assertNotEquals(negateOperator, negateOperator2);
    Assertions.assertNotEquals(negateOperator.hashCode(), negateOperator2.hashCode());
    Assertions.assertNotEquals(negateOperator,this);

  }

  @Test
  void simpleErrorTests()  {
    Assertions.assertThrows(ParseException.class, ()->{ new NegateOperator(null);});
  }

}
