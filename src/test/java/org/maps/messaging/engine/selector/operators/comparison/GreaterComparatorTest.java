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

package org.maps.messaging.engine.selector.operators.comparison;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.messaging.engine.selector.ParseException;

public class GreaterComparatorTest {
  Object[][] SUCCESS_VALUES = {{4l,3l},{4l,2.1},{3.9,2l}, {5.0,2.0} };
  Object[][] FAILURE_VALUES = {{5l,30l},{2l,3.1},{1.9,2l}, {2.0,2.1}, {"42", "2"}, {false, true}, {true, false}, {"2", "3"}, {true, true}, {false, false} };

  @Test
  public void simpleValidationTest() throws ParseException {
    for(Object[] values:SUCCESS_VALUES){
      GreaterThanOperator greaterThanOperator = new GreaterThanOperator(values[0], values[1]);
      Assertions.assertTrue((Boolean) greaterThanOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
    for(Object[] values:FAILURE_VALUES){
      GreaterThanOperator greaterThanOperator = new GreaterThanOperator(values[0], values[1]);
      Assertions.assertFalse((Boolean) greaterThanOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
  }
}
