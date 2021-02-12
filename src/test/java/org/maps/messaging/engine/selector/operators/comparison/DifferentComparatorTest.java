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

public class DifferentComparatorTest {

  Object[][] FAILURE_VALUES = {{2l,2l},{2l,2.0},{2.0,2l}, {2.0,2.0}, {"2", "2"}, {true, true}, {false, false} };
  Object[][] SUCCESS_VALUES = {{1l,2l},{1l,2.0},{1.0,2l}, {1.0,2.0}, {"1", "2"}, {false, true}, {true, false} };

  @Test
  public void simpleValidationTest() throws ParseException {
    for(Object[] values:SUCCESS_VALUES){
      DifferentOperator differentOperator = new DifferentOperator(values[0], values[1]);
      Assertions.assertTrue((Boolean) differentOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }
    for(Object[] values:FAILURE_VALUES){
      DifferentOperator differentOperator = new DifferentOperator(values[0], values[1]);
      Assertions.assertFalse((Boolean) differentOperator.evaluate(null), "Failed on {"+values[0]+","+values[1]+"}");
    }

  }
}
