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

public class SubtractionTest {
  // test the different paths (Long + Long, Double+Long, Long+Double and Double + Double)
  @Test
  public void simpleMathTests() throws ParseException {
    SubtractOperator subtractOperator = new SubtractOperator(8l, 2l);
    Assertions.assertEquals(6l, subtractOperator.evaluate(null));

    subtractOperator = new SubtractOperator(2.4, 2l);
    double check = 0.4 - ((Double)subtractOperator.evaluate(null));
    Assertions.assertTrue(check < 0.01);

    subtractOperator = new SubtractOperator(2.4, 2.4);
    Assertions.assertEquals(0.0, subtractOperator.evaluate(null));

    subtractOperator = new SubtractOperator(2l, 2.4);
    check = -0.4 - ((Double)subtractOperator.evaluate(null));
    Assertions.assertTrue(check < 0.01);

    Assertions.assertEquals("(2) - (2.4)", subtractOperator.toString());

    subtractOperator = new SubtractOperator(2l, 2.4);
    SubtractOperator subtractOperator2 = new SubtractOperator(2l, 2.4);
    Assertions.assertEquals(subtractOperator, subtractOperator2);
    Assertions.assertEquals(subtractOperator.hashCode(), subtractOperator2.hashCode());

    subtractOperator2 = new SubtractOperator(2l, 240);
    Assertions.assertNotEquals(subtractOperator, subtractOperator2);
    Assertions.assertNotEquals(subtractOperator.hashCode(), subtractOperator2.hashCode());
    Assertions.assertNotEquals(subtractOperator, this);

  }

  @Test
  void simpleErrorTests()  {
    Assertions.assertThrows(ParseException.class, ()->{ new SubtractOperator(2L, "fred");});
    Assertions.assertThrows(ParseException.class, ()->{ new SubtractOperator("fred", 2L);});
  }

}
