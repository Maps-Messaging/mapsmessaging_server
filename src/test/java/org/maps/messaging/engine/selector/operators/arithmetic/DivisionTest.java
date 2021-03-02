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

package org.maps.messaging.engine.selector.operators.arithmetic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.messaging.engine.selector.ParseException;

public class DivisionTest {

  // test the different paths (Long + Long, Double+Long, Long+Double and Double + Double)
  @Test
  public void simpleMathTests() throws ParseException {
    DivideOperator divideOperator = new DivideOperator(8l, 2l);
    Assertions.assertEquals(4l, divideOperator.evaluate(null));

    divideOperator = new DivideOperator(4.8, 2l);
    Assertions.assertEquals(2.4, divideOperator.evaluate(null));

    divideOperator = new DivideOperator(2.4, 2.4);
    Assertions.assertEquals(1.0, divideOperator.evaluate(null));

    divideOperator = new DivideOperator(20l, 2.0);
    Assertions.assertEquals(10.0, divideOperator.evaluate(null));

    divideOperator = new DivideOperator(20l, 0);
    Assertions.assertEquals(Double.NaN, divideOperator.evaluate(null));
  }

  @Test
  void simpleErrorTests()  {
    Assertions.assertThrows(ParseException.class, ()->{ new DivideOperator(2L, "fred");});
    Assertions.assertThrows(ParseException.class, ()->{ new DivideOperator("fred", 2L);});
  }

}
