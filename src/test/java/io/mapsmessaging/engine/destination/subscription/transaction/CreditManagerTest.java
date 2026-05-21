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

package io.mapsmessaging.engine.destination.subscription.transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CreditManagerTest {

  @Test
  void fixedCreditManager_whenInitialCreditIsLessThanOne_defaultsTo32() {
    FixedCreditManager creditManager = new FixedCreditManager(0);
    Assertions.assertEquals(32, creditManager.getCurrentCredit());

    FixedCreditManager creditManagerNegative = new FixedCreditManager(-5);
    Assertions.assertEquals(32, creditManagerNegative.getCurrentCredit());
  }

  @Test
  void fixedCreditManager_whenInitialCreditIsValid_keepsValue() {
    FixedCreditManager creditManager = new FixedCreditManager(1);
    Assertions.assertEquals(1, creditManager.getCurrentCredit());

    FixedCreditManager creditManagerLarge = new FixedCreditManager(100);
    Assertions.assertEquals(100, creditManagerLarge.getCurrentCredit());
  }

  @Test
  void fixedCreditManager_incrementAndDecrement_doNotChangeCredit() {
    FixedCreditManager creditManager = new FixedCreditManager(10);

    creditManager.decrement();
    creditManager.decrement();
    creditManager.increment();
    creditManager.increment();

    Assertions.assertEquals(10, creditManager.getCurrentCredit());
  }

  @Test
  void clientCreditManager_decrement_reducesCredit() {
    ClientCreditManager creditManager = new ClientCreditManager(3);

    Assertions.assertEquals(3, creditManager.getCurrentCredit());

    creditManager.decrement();
    Assertions.assertEquals(2, creditManager.getCurrentCredit());

    creditManager.decrement();
    Assertions.assertEquals(1, creditManager.getCurrentCredit());

    creditManager.decrement();
    Assertions.assertEquals(0, creditManager.getCurrentCredit());
  }

  @Test
  void clientCreditManager_increment_doesNotChangeCredit() {
    ClientCreditManager creditManager = new ClientCreditManager(3);

    creditManager.increment();
    Assertions.assertEquals(3, creditManager.getCurrentCredit());

    creditManager.decrement();
    Assertions.assertEquals(2, creditManager.getCurrentCredit());

    creditManager.increment();
    Assertions.assertEquals(2, creditManager.getCurrentCredit());
  }

  @Test
  void setCurrentCredit_updatesValue() {
    ClientCreditManager creditManager = new ClientCreditManager(5);

    Assertions.assertEquals(5, creditManager.getCurrentCredit());

    creditManager.setCurrentCredit(10);
    Assertions.assertEquals(10, creditManager.getCurrentCredit());

    creditManager.setCurrentCredit(0);
    Assertions.assertEquals(0, creditManager.getCurrentCredit());
  }

  @Test
  void setCurrentCredit_sameValue_doesNotChangeObservedValue() {
    ClientCreditManager creditManager = new ClientCreditManager(5);

    creditManager.setCurrentCredit(5);

    Assertions.assertEquals(5, creditManager.getCurrentCredit());
  }

  @Test
  void clientCreditManager_canGoNegative_ifDecrementCalledTooOften() {
    ClientCreditManager creditManager = new ClientCreditManager(1);

    creditManager.decrement();
    Assertions.assertEquals(0, creditManager.getCurrentCredit());

    creditManager.decrement();
    Assertions.assertEquals(-1, creditManager.getCurrentCredit());
  }

  @Test
  void fixedCreditManager_setCurrentCredit_canOverrideEvenThoughIncrementDecrementAreNoOps() {
    FixedCreditManager creditManager = new FixedCreditManager(10);

    creditManager.setCurrentCredit(99);

    Assertions.assertEquals(99, creditManager.getCurrentCredit());

    creditManager.decrement();
    creditManager.increment();

    Assertions.assertEquals(99, creditManager.getCurrentCredit());
  }
}
