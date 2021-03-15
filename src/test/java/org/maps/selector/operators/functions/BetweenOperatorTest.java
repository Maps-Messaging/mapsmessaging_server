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

package org.maps.selector.operators.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.selector.ParseException;

public class BetweenOperatorTest {


  @Test
  public void simpleValidation() throws ParseException {
    BetweenOperator betweenOperator = new BetweenOperator(12L, 10L, 20L);
    Assertions.assertTrue( (Boolean) betweenOperator.evaluate(null), "Failed on numeric tests");

    betweenOperator = new BetweenOperator(12L, 15L, 20L);
    Assertions.assertFalse( (Boolean) betweenOperator.evaluate(null), "Failed on numeric tests");

    // This can not be evaluated since strings can not be compared besides = and !=
    betweenOperator = new BetweenOperator("12", "10", "20");
    Assertions.assertFalse( (Boolean) betweenOperator.evaluate(null), "Failed on numeric tests");

    Assertions.assertNotEquals(betweenOperator, this);
  }
}
