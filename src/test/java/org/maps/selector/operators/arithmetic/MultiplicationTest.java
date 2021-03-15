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

public class MultiplicationTest {
  // test the different paths (Long + Long, Double+Long, Long+Double and Double + Double)
  @Test
  public void simpleMathTests() throws ParseException {
    MultiplyOperator multiplyOperator = new MultiplyOperator(8l, 2l);
    Assertions.assertEquals(16l, multiplyOperator.evaluate(null));

    multiplyOperator = new MultiplyOperator(4.8, 2l);
    Assertions.assertEquals(9.6, multiplyOperator.evaluate(null));

    multiplyOperator = new MultiplyOperator(2.4, 2.4);
    Assertions.assertEquals(5.76, multiplyOperator.evaluate(null));

    multiplyOperator = new MultiplyOperator(20l, 2.0);
    Assertions.assertEquals(40.0, multiplyOperator.evaluate(null));

    multiplyOperator = new MultiplyOperator(20l, 0);
    Assertions.assertEquals(0L, multiplyOperator.evaluate(null));
    Assertions.assertEquals("(20) * (0)", multiplyOperator.toString());

    multiplyOperator = new MultiplyOperator(2L, 2.4);
    MultiplyOperator multiplyOperator2 = new MultiplyOperator(2L, 2.4);
    Assertions.assertEquals(multiplyOperator, multiplyOperator2);
    Assertions.assertEquals(multiplyOperator.hashCode(), multiplyOperator2.hashCode());

    multiplyOperator2 = new MultiplyOperator(2L, 240);
    Assertions.assertNotEquals(multiplyOperator, multiplyOperator2);
    Assertions.assertNotEquals(multiplyOperator.hashCode(), multiplyOperator2.hashCode());
    Assertions.assertNotEquals(multiplyOperator, this);

  }

  @Test
  void simpleErrorTests()  {
    Assertions.assertThrows(ParseException.class, ()->{ new MultiplyOperator(2L, "fred");});
    Assertions.assertThrows(ParseException.class, ()->{ new MultiplyOperator("fred", 2L);});
  }

}
