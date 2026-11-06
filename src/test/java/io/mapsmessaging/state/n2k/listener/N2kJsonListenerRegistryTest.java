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

package io.mapsmessaging.state.n2k.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class N2kJsonListenerRegistryTest {

  @Test
  void knownPgnsResolveToExpectedListenerTypes() {
    N2kJsonListenerRegistry registry = new N2kJsonListenerRegistry();

    assertInstanceOf(N2kPositionJsonListener.class, registry.getListener(N2kPgns.POSITION_RAPID_UPDATE));
    assertInstanceOf(N2kGnssJsonListener.class, registry.getListener(N2kPgns.GNSS_POSITION_DATA));
    assertInstanceOf(N2kMotionJsonListener.class, registry.getListener(N2kPgns.COG_SOG_RAPID_UPDATE));
    assertInstanceOf(N2kHeadingJsonListener.class, registry.getListener(N2kPgns.VESSEL_HEADING));
    assertInstanceOf(N2kAttitudeJsonListener.class, registry.getListener(N2kPgns.ATTITUDE));
    assertInstanceOf(N2kGnssDopsJsonListener.class, registry.getListener(N2kPgns.GNSS_DOPS));
    assertInstanceOf(N2kBatteryStatusJsonListener.class, registry.getListener(N2kPgns.BATTERY_STATUS));
    assertInstanceOf(N2kWindJsonListener.class, registry.getListener(N2kPgns.WIND_DATA));
  }

  @Test
  void unknownPgnHasNoListener() {
    N2kJsonListenerRegistry registry = new N2kJsonListenerRegistry();

    assertFalse(registry.hasListener(Integer.MAX_VALUE));
    assertNull(registry.getListener(Integer.MAX_VALUE));
  }
}
