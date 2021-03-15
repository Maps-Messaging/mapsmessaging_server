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

import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.selector.ParseException;

public class InOperatorTest {


  @Test
  public void simpleInValidation() throws ParseException {

    String[] list = {"not this", "nor this", "maybe this HI", "HI"};
    InOperator inOperator = new InOperator("HI", new HashSet<>(Arrays.asList(list)));
    Assertions.assertTrue((Boolean)inOperator.evaluate(null));

    inOperator = new InOperator("fail", new HashSet<>(Arrays.asList(list)));
    Assertions.assertFalse((Boolean)inOperator.evaluate(null));
    Assertions.assertNotEquals(inOperator, this);


  }

}
