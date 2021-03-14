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

public class OrLogicTest {
  @Test
  public void simpleValidation() throws ParseException {
    OrOperator orOperator = new OrOperator(true, true);
    Assertions.assertTrue( (Boolean) orOperator.evaluate(null), "Failed on simple tests tests");

    orOperator = new OrOperator(true, false);
    Assertions.assertTrue( (Boolean) orOperator.evaluate(null), "Failed on simple tests tests");


    orOperator = new OrOperator(false, true);
    Assertions.assertTrue( (Boolean) orOperator.evaluate(null), "Failed on simple tests tests");

    orOperator = new OrOperator(false, false);
    Assertions.assertFalse( (Boolean) orOperator.evaluate(null), "Failed on simple tests tests");


    orOperator = new OrOperator(true, true);
    Assertions.assertTrue( (Boolean) orOperator.compile(), "Failed on simple tests tests");

    orOperator = new OrOperator(true, false);
    Assertions.assertTrue( (Boolean) orOperator.compile(), "Failed on simple tests tests");


    orOperator = new OrOperator(false, true);
    Assertions.assertTrue( (Boolean) orOperator.compile(), "Failed on simple tests tests");

    orOperator = new OrOperator(false, false);
    Assertions.assertFalse( (Boolean) orOperator.compile(), "Failed on simple tests tests");

  }

  @Test
  void checkEquivalence(){
    OrOperator orOperator1 = new OrOperator(true, true);
    OrOperator orOperator2 = new OrOperator(true, true);
    Assertions.assertEquals(orOperator1, orOperator2);

    orOperator1 = new OrOperator(false, false);
    orOperator2 = new OrOperator(false, false);
    Assertions.assertEquals(orOperator1, orOperator2);

    orOperator1 = new OrOperator(true, true);
    orOperator2 = new OrOperator(false, false);
    Assertions.assertNotEquals(orOperator1, orOperator2);


    orOperator1 = new OrOperator(true, false);
    orOperator2 = new OrOperator(false, true);
    Assertions.assertNotEquals(orOperator1, orOperator2);

    orOperator1 = new OrOperator(true, false);
    Assertions.assertNotEquals(orOperator1, this);
  }
}
