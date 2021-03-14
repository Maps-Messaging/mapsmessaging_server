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

package org.maps.selector.operators.logical;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.selector.ParseException;

class AndLogicTest {
  @Test
  void simpleValidation() throws ParseException {
    AndOperator andOperator = new AndOperator(true, true);
    Assertions.assertTrue( (Boolean) andOperator.evaluate(null), "Failed on simple tests tests");

    andOperator = new AndOperator(true, false);
    Assertions.assertFalse( (Boolean) andOperator.evaluate(null), "Failed on simple tests tests");

    andOperator = new AndOperator(false, true);
    Assertions.assertFalse( (Boolean) andOperator.evaluate(null), "Failed on simple tests tests");

    andOperator = new AndOperator(false, false);
    Assertions.assertFalse( (Boolean) andOperator.evaluate(null), "Failed on simple tests tests");

    andOperator = new AndOperator(true, true);
    Assertions.assertTrue( (Boolean) andOperator.compile(), "Failed on simple tests tests");

    andOperator = new AndOperator(true, false);
    Assertions.assertFalse( (Boolean) andOperator.compile(), "Failed on simple tests tests");

    andOperator = new AndOperator(false, true);
    Assertions.assertFalse( (Boolean) andOperator.compile(), "Failed on simple tests tests");

    andOperator = new AndOperator(false, false);
    Assertions.assertFalse( (Boolean) andOperator.compile(), "Failed on simple tests tests");
  }

  @Test
  void checkEquivalence(){
    AndOperator andOperator1 = new AndOperator(true, true);
    AndOperator andOperator2 = new AndOperator(true, true);
    Assertions.assertEquals(andOperator2, andOperator1);

    andOperator1 = new AndOperator(false, false);
    andOperator2 = new AndOperator(false, false);
    Assertions.assertEquals(andOperator2, andOperator1);

    andOperator1 = new AndOperator(true, true);
    andOperator2 = new AndOperator(false, false);
    Assertions.assertNotEquals(andOperator2, andOperator1);


    andOperator1 = new AndOperator(false, true);
    andOperator2 = new AndOperator(true, false);
    Assertions.assertNotEquals(andOperator2, andOperator1);

    andOperator2 = new AndOperator(true, false);
    Assertions.assertNotEquals(andOperator2, this);
  }

}
