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

package io.mapsmessaging.network.protocol.impl.semtech.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SemtechStatusEventFactoryTest {

  private final SemtechStatusEventFactory factory = SemtechStatusEventFactory.getInstance();

  @ParameterizedTest
  @EnumSource(SemtechStatusState.class)
  void gateway_factory_accepts_only_existing_gateway_states(SemtechStatusState state) {
    boolean accepted = switch (state) {
      case GATEWAY_REGISTERED, GATEWAY_PULL, GATEWAY_EXPIRED, GATEWAY_ADDRESS_CHANGED -> true;
      default -> false;
    };
    if (accepted) {
      SemtechStatusEvent event = factory.createGatewayEvent("gateway", state);
      assertEquals(state, event.getState());
      assertEquals(SemtechStatusType.GATEWAY, event.getType());
    } else {
      assertThrows(IllegalArgumentException.class, () -> factory.createGatewayEvent("gateway", state));
    }
  }

  @ParameterizedTest
  @EnumSource(SemtechStatusState.class)
  void downlink_factory_accepts_downlink_states_and_rejects_gateway_states(SemtechStatusState state) {
    if (state.name().startsWith("DOWNLINK_")) {
      SemtechStatusEvent event = factory.createDownlinkEvent("gateway", state);
      assertEquals(state, event.getState());
      assertEquals(SemtechStatusType.DOWNLINK, event.getType());
    } else {
      assertThrows(IllegalArgumentException.class, () -> factory.createDownlinkEvent("gateway", state));
    }
  }

  @Test
  void both_factories_reject_null_states() {
    assertThrows(NullPointerException.class, () -> factory.createGatewayEvent("gateway", null));
    assertThrows(NullPointerException.class, () -> factory.createDownlinkEvent("gateway", null));
  }
}
