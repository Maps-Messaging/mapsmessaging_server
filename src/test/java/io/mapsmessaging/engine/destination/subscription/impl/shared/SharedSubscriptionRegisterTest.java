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

package io.mapsmessaging.engine.destination.subscription.impl.shared;

import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SharedSubscriptionRegisterTest {

  @Test
  void newRegister_getUnknown_returnsNull() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();
    Assertions.assertNull(register.get("missing"));
  }

  @Test
  void add_thenGet_returnsSameInstance() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();
    FakeSharedSubscriptionManager manager = new FakeSharedSubscriptionManager();

    register.add("groupA", manager);

    Assertions.assertSame(manager, register.get("groupA"));
  }

  @Test
  void add_existingKey_returnsPrevious_andReplaces() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();
    FakeSharedSubscriptionManager first = new FakeSharedSubscriptionManager();
    FakeSharedSubscriptionManager second = new FakeSharedSubscriptionManager();

    Assertions.assertNull(register.add("groupA", first));

    FakeSharedSubscriptionManager previous = (FakeSharedSubscriptionManager) register.add("groupA", second);
    Assertions.assertSame(first, previous);
    Assertions.assertSame(second, register.get("groupA"));
  }

  @Test
  void del_removesEntry() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();
    FakeSharedSubscriptionManager manager = new FakeSharedSubscriptionManager();

    register.add("groupA", manager);
    Assertions.assertNotNull(register.get("groupA"));

    register.del("groupA");
    Assertions.assertNull(register.get("groupA"));
  }

  @Test
  void del_unknownKey_isNoop() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();
    register.del("missing"); // should not throw
    Assertions.assertNull(register.get("missing"));
  }

  @Test
  void getState_emptyRegister_returnsEmptyList() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();
    List<SubscriptionStateDTO> states = register.getState();

    Assertions.assertNotNull(states);
    Assertions.assertTrue(states.isEmpty());
  }

  @Test
  void getState_flattensAllManagerStates_inInsertionOrder() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();

    SubscriptionStateDTO a1 = new SubscriptionStateDTO();
    SubscriptionStateDTO a2 = new SubscriptionStateDTO();
    SubscriptionStateDTO b1 = new SubscriptionStateDTO();

    FakeSharedSubscriptionManager managerA = new FakeSharedSubscriptionManager(List.of(a1, a2));
    FakeSharedSubscriptionManager managerB = new FakeSharedSubscriptionManager(List.of(b1));

    register.add("A", managerA);
    register.add("B", managerB);

    List<SubscriptionStateDTO> combined = register.getState();

    Assertions.assertEquals(3, combined.size());
    Assertions.assertSame(a1, combined.get(0));
    Assertions.assertSame(a2, combined.get(1));
    Assertions.assertSame(b1, combined.get(2));
  }

  @Test
  void getState_managerReturnsEmptyList_stillWorks() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();

    FakeSharedSubscriptionManager emptyManager = new FakeSharedSubscriptionManager(Collections.emptyList());
    register.add("A", emptyManager);

    List<SubscriptionStateDTO> combined = register.getState();

    Assertions.assertNotNull(combined);
    Assertions.assertTrue(combined.isEmpty());
  }

  @Test
  void getState_managerReturnsNullList_throwsNullPointerException() {
    SharedSubscriptionRegister register = new SharedSubscriptionRegister();

    FakeSharedSubscriptionManager nullManager = new FakeSharedSubscriptionManager(null);
    register.add("A", nullManager);

    Assertions.assertThrows(NullPointerException.class, register::getState);
  }

  // -----------------------------------------------------------------------
  // Fake SharedSubscriptionManager
  // -----------------------------------------------------------------------
  private static final class FakeSharedSubscriptionManager extends SharedSubscriptionManager {

    private final List<SubscriptionStateDTO> states;

    private FakeSharedSubscriptionManager() {
      super("name");
      this.states = new ArrayList<>();
    }

    private FakeSharedSubscriptionManager(List<SubscriptionStateDTO> states) {
      super("name");
      this.states = states;
    }

    @Override
    public List<SubscriptionStateDTO> getSubscriptionStates() {
      return states;
    }
  }
}
