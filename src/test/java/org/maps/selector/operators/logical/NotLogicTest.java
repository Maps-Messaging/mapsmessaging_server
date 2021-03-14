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

public class NotLogicTest {
  @Test
  public void simpleValidation() throws ParseException {
    NotOperator notOperator = new NotOperator(false);
    Assertions.assertTrue( (Boolean) notOperator.evaluate(null), "Failed on simple tests tests");

    notOperator = new NotOperator(true);
    Assertions.assertFalse( (Boolean) notOperator.evaluate(null), "Failed on simple tests tests");

    notOperator = new NotOperator(false);
    Assertions.assertTrue( (Boolean) notOperator.compile(), "Failed on simple tests tests");

    notOperator = new NotOperator(true);
    Assertions.assertFalse( (Boolean) notOperator.compile(), "Failed on simple tests tests");

  }

  @Test
  void checkEquivalence(){
    NotOperator notOperator1 = new NotOperator(true);
    NotOperator notOperator2 = new NotOperator(true );
    Assertions.assertEquals(notOperator1, notOperator2);

    notOperator1 = new NotOperator(false);
    notOperator2 = new NotOperator(false );
    Assertions.assertEquals(notOperator1, notOperator2);

    notOperator1 = new NotOperator(true);
    notOperator2 = new NotOperator(false );
    Assertions.assertNotEquals(notOperator1, notOperator2);

    notOperator1 = new NotOperator(true);
    Assertions.assertNotEquals(notOperator1, this);
  }
}
