/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package org.maps.selector.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NumericValidatorTest {
  @Test
  @DisplayName("Check valid numbers")
  void checkValid() {
    Assertions.assertTrue(NumericValidator.isValid(1));
    Assertions.assertTrue(NumericValidator.isValid(1.0f));
    Assertions.assertTrue(NumericValidator.isValid(1.0));
    Assertions.assertTrue(NumericValidator.isValid(1L));
    Assertions.assertTrue(NumericValidator.isValid((byte)1));
    Assertions.assertTrue(NumericValidator.isValid((short)1));
  }

  @Test
  @DisplayName("Check invalid numbers")
  void checkInvalid() {
    Assertions.assertFalse(NumericValidator.isValid("1"));
    Assertions.assertFalse(NumericValidator.isValid(true));
    Assertions.assertFalse(NumericValidator.isValid(this));
  }
}
