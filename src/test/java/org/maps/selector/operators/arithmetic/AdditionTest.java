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

class AdditionTest {


  // test the different paths (Long + Long, Double+Long, Long+Double and Double + Double)
  @Test
  void simpleMathTests() throws ParseException {
    AddOperator addOperator = new AddOperator(2l, 2l);
    Assertions.assertEquals(4l, addOperator.evaluate(null));

    addOperator = new AddOperator(2.4, 2l);
    Assertions.assertEquals(4.4, addOperator.evaluate(null));

    addOperator = new AddOperator(2.4, 2.4);
    Assertions.assertEquals(4.8, addOperator.evaluate(null));

    addOperator = new AddOperator(2l, 2.4);
    Assertions.assertEquals(4.4, addOperator.evaluate(null));
    Assertions.assertEquals("(2) + (2.4)", addOperator.toString());

    addOperator = new AddOperator(2l, 2.4);
    AddOperator addOperator2 = new AddOperator(2l, 2.4);
    Assertions.assertEquals(addOperator, addOperator2);
    Assertions.assertEquals(addOperator.hashCode(), addOperator2.hashCode());

    addOperator2 = new AddOperator(2l, 240);
    Assertions.assertNotEquals(addOperator, addOperator2);
    Assertions.assertNotEquals(addOperator.hashCode(), addOperator2.hashCode());
    Assertions.assertNotEquals(addOperator, this);

  }

  @Test
  void simpleErrorTests()  {
    Assertions.assertThrows(ParseException.class, ()->{ new AddOperator(2L, "fred");});
    Assertions.assertThrows(ParseException.class, ()->{ new AddOperator("fred", 2L);});
  }
}
