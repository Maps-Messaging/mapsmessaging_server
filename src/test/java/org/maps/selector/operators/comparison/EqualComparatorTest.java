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

public class EqualComparatorTest extends ComparisonOperatorTest {

  Object[][] SUCCESS_VALUES = {{2l,2l},{3l,3.0},{4.0,4l}, {5.0,5.0}, {"20", "20"}, {true, true}, {false, false} };
  Object[][] FAILURE_VALUES = {{1l,2l},{1l,2.0},{1.0,2l}, {1.0,2.0}, {"1", "2"}, {false, true}, {true, false} };

  @Test
  public void simpleValidationTest() throws ParseException {
    for(Object[] values:SUCCESS_VALUES){
      EqualOperator equalOperator = new EqualOperator(values[0], values[1]);
      Assertions.assertTrue((Boolean) equalOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
    for(Object[] values:FAILURE_VALUES){
      EqualOperator equalOperator = new EqualOperator(values[0], values[1]);
      Assertions.assertFalse((Boolean) equalOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
  }

  @Test
  void simpleEquality(){
    EqualOperator equalOperator = new EqualOperator(10.0, 10);
    Assertions.assertEquals("(10.0) == (10)", equalOperator.toString());

    EqualOperator equalOperator2 = new EqualOperator(10.0, 10);
    Assertions.assertEquals( equalOperator, equalOperator2);
    Assertions.assertEquals( equalOperator.hashCode(), equalOperator2.hashCode());

    equalOperator2 = new EqualOperator(10.0, 20.2);
    Assertions.assertNotEquals( equalOperator, equalOperator2);
    Assertions.assertNotEquals( equalOperator.hashCode(), equalOperator2.hashCode());
    Assertions.assertNotEquals( equalOperator, this);
  }

  @Test
  void evaluationCheck() throws ParseException {
    EqualOperator equalOperator = new EqualOperator(new Identifier("textKey"),"text data");
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("textKey"),"text 1 data");
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);

    equalOperator = new EqualOperator(new Identifier("textNumericLongKey"),102L);
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("textNumericLongKey"),122L);
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);


    equalOperator = new EqualOperator(102L, new Identifier("textNumericLongKey"));
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(122L, (new Identifier("textNumericLongKey")));
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);


    equalOperator = new EqualOperator(new Identifier("textNumericRealKey"),102.11);
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("textNumericRealKey"),102.12);
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);


    equalOperator = new EqualOperator(new Identifier("longKey"),1002L);
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("longKey"),102.12);
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);


    equalOperator = new EqualOperator(new Identifier("intKey"),1002L);
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("intKey"),102.12);
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);

    equalOperator = new EqualOperator(new Identifier("shortKey"),1002L);
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("shortKey"),102.12);
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);

    equalOperator = new EqualOperator(new Identifier("byteKey"),0x1F);
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("byteKey"),102.12);
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);

    equalOperator = new EqualOperator(new Identifier("doubleKey"),1002.12);
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("doubleKey"),102.12);
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);

    equalOperator = new EqualOperator(new Identifier("floatKey"),1002.12f);
    Assertions.assertEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
    equalOperator = new EqualOperator(new Identifier("floatKey"),102.12);
    Assertions.assertNotEquals(equalOperator.evaluate(getResolver()), Boolean.TRUE);
  }
}
