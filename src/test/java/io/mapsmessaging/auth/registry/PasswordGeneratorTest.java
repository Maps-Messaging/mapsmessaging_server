/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.auth.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {

  @Test
  void generateRandomPassword_returnsRequestedLengthAndAllowedCharacters() {
    String password = PasswordGenerator.generateRandomPassword(512);

    assertEquals(512, password.length());
    assertTrue(password.matches("[A-Za-z0-9!@#$%]+"));
  }

  @Test
  void generateSalt_returnsRequestedLengthAndAlphaNumericCharactersOnly() {
    String salt = PasswordGenerator.generateSalt(512);

    assertEquals(512, salt.length());
    assertTrue(salt.matches("[A-Za-z0-9]+"));
  }

  @Test
  void zeroLength_requestsReturnEmptyStrings() {
    assertEquals("", PasswordGenerator.generateRandomPassword(0));
    assertEquals("", PasswordGenerator.generateSalt(0));
  }

  @Test
  void negativeLength_requestsReturnEmptyStrings() {
    assertEquals("", PasswordGenerator.generateRandomPassword(-1));
    assertEquals("", PasswordGenerator.generateSalt(-1));
  }
}
