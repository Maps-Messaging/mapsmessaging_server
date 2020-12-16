/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.engine.selector.operators.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.messaging.engine.selector.Identifier;
import org.maps.messaging.engine.selector.ParseException;

public class IsOperatorTest {


  @Test
  public void basicIsOperations() throws ParseException {

    //Since we pass a string it should NEVER be NULL
    IsOperator isOperator = new IsOperator("llhs", false);
    Assertions.assertFalse((Boolean) isOperator.evaluate(null));

    isOperator = new IsOperator("llhs", true);
    Assertions.assertTrue((Boolean) isOperator.evaluate(null));


    isOperator = new IsOperator(new Identifier("lhs"), false);
    Assertions.assertTrue((Boolean) isOperator.evaluate(null));

    isOperator = new IsOperator(new Identifier("lhs"), true);
    Assertions.assertFalse((Boolean) isOperator.evaluate(null));

  }
}
