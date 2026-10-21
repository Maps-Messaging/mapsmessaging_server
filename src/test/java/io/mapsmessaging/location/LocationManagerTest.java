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
package io.mapsmessaging.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocationManagerTest {

  @Test
  void getInstance_returnsSameManager() {
    assertSame(LocationManager.getInstance(), LocationManager.getInstance());
  }

  @Test
  void setPosition_storesCoordinatesAndReplacesPreviousPosition() {
    LocationManager locationManager = LocationManager.getInstance();

    locationManager.setPosition(-90.0d, 180.0d);

    assertTrue(locationManager.isSet());
    assertEquals(-90.0d, locationManager.getLatitude());
    assertEquals(180.0d, locationManager.getLongitude());

    locationManager.setPosition(51.5074d, -0.1278d);

    assertEquals(51.5074d, locationManager.getLatitude());
    assertEquals(-0.1278d, locationManager.getLongitude());
  }
}
