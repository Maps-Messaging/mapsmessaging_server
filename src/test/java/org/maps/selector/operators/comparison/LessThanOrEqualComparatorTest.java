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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.selector.Identifier;
import org.maps.selector.ParseException;

public class LessThanOrEqualComparatorTest  extends ComparisonOperatorTest {

  Object[][] SUCCESS_VALUES = {{5l,30l},{2l,3.1},{1.9,2l}, {2.0,2.1}, {5L, 5L}, {10L, 10.0}, {10.0, 10}, {10.0, 10.0}};
  Object[][] FAILURE_VALUES = {{4l,3l},{4l,2.1},{3.9,2l}, {5.0,2.0}, {"42", "2"}, {false, true}, {true, false}, {"2", "3"}, {true, true}, {false, false}  };

  @Test
  public void simpleValidationTest() throws ParseException {
    for(Object[] values:SUCCESS_VALUES){
      LessOrEqualOperator lessOrEqualOperator = new LessOrEqualOperator(values[0], values[1]);
      Assertions.assertTrue((Boolean) lessOrEqualOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
    for(Object[] values:FAILURE_VALUES){
      LessOrEqualOperator lessOrEqualOperator = new LessOrEqualOperator(values[0], values[1]);
      Assertions.assertFalse((Boolean) lessOrEqualOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
  }
  @Test
  void simpleEquality(){
    LessOrEqualOperator LessOrEqualOperator = new LessOrEqualOperator(10.0, 10);
    Assertions.assertEquals("(10.0) <= (10)", LessOrEqualOperator.toString());

    LessOrEqualOperator LessOrEqualOperator2 = new LessOrEqualOperator(10.0, 10);
    Assertions.assertEquals( LessOrEqualOperator, LessOrEqualOperator2);
    Assertions.assertEquals( LessOrEqualOperator.hashCode(), LessOrEqualOperator2.hashCode());

    LessOrEqualOperator2 = new LessOrEqualOperator(10.0, 20.2);
    Assertions.assertNotEquals( LessOrEqualOperator, LessOrEqualOperator2);
    Assertions.assertNotEquals( LessOrEqualOperator.hashCode(), LessOrEqualOperator2.hashCode());
    Assertions.assertNotEquals( LessOrEqualOperator, this);
  }

  @Test
  void evaluationCheck() throws ParseException {
    LessOrEqualOperator operator = new LessOrEqualOperator(new Identifier("textKey"),"text data");
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("textKey"),"text 1 data");
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessOrEqualOperator(new Identifier("textNumericLongKey"),10L);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("textNumericLongKey"),122L);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessOrEqualOperator(new Identifier("textNumericRealKey"),10.11);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("textNumericRealKey"),102.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);


    operator = new LessOrEqualOperator(new Identifier("longKey"),10L);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("longKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);


    operator = new LessOrEqualOperator(new Identifier("intKey"),10L);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("intKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessOrEqualOperator(new Identifier("shortKey"),10L);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("shortKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessOrEqualOperator(new Identifier("byteKey"),0x1);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("byteKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessOrEqualOperator(new Identifier("doubleKey"),10.12);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("doubleKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessOrEqualOperator(new Identifier("floatKey"),10.12f);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessOrEqualOperator(new Identifier("floatKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);
  }
}
