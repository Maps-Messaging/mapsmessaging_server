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

public class LessThanComparatorTest  extends ComparisonOperatorTest {
  Object[][] SUCCESS_VALUES = {{5l,30l},{2l,3.1},{1.9,2l}, {2.0,2.1}};
  Object[][] FAILURE_VALUES = {{4l,3l},{4l,2.1},{3.9,2l}, {5.0,2.0}, {"42", "2"}, {false, true}, {true, false}, {"2", "3"}, {true, true}, {false, false}  };

  @Test
  public void simpleValidationTest() throws ParseException {
    for(Object[] values:SUCCESS_VALUES){
      LessThanOperator lessThanOperator = new LessThanOperator(values[0], values[1]);
      Assertions.assertTrue((Boolean) lessThanOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
    for(Object[] values:FAILURE_VALUES){
      LessThanOperator lessThanOperator = new LessThanOperator(values[0], values[1]);
      Assertions.assertFalse((Boolean) lessThanOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
  }
  
  @Test
  void simpleEquality(){
    LessThanOperator LessThanOperator = new LessThanOperator(10.0, 10);
    Assertions.assertEquals("(10.0) < (10)", LessThanOperator.toString());

    LessThanOperator LessThanOperator2 = new LessThanOperator(10.0, 10);
    Assertions.assertEquals( LessThanOperator, LessThanOperator2);
    Assertions.assertEquals( LessThanOperator.hashCode(), LessThanOperator2.hashCode());

    LessThanOperator2 = new LessThanOperator(10.0, 20.2);
    Assertions.assertNotEquals( LessThanOperator, LessThanOperator2);
    Assertions.assertNotEquals( LessThanOperator.hashCode(), LessThanOperator2.hashCode());
    Assertions.assertNotEquals( LessThanOperator, this);
  }

  @Test
  void evaluationCheck() throws ParseException {
    LessThanOperator operator = new LessThanOperator(new Identifier("textKey"),"text data");
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("textKey"),"text 1 data");
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessThanOperator(new Identifier("textNumericLongKey"),10L);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("textNumericLongKey"),122L);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessThanOperator(new Identifier("textNumericRealKey"),10.11);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("textNumericRealKey"),102.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);


    operator = new LessThanOperator(new Identifier("longKey"),10L);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("longKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);


    operator = new LessThanOperator(new Identifier("intKey"),10L);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("intKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessThanOperator(new Identifier("shortKey"),10L);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("shortKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessThanOperator(new Identifier("byteKey"),0x1);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("byteKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessThanOperator(new Identifier("doubleKey"),10.12);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("doubleKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);

    operator = new LessThanOperator(new Identifier("floatKey"),10.12f);
    Assertions.assertNotEquals(operator.evaluate(getResolver()), Boolean.TRUE);
    operator = new LessThanOperator(new Identifier("floatKey"),10200.12);
    Assertions.assertEquals(operator.evaluate(getResolver()), Boolean.TRUE);
  }
}
