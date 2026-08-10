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
package io.mapsmessaging.state.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataProductConfigTest {

  @Test
  void droneInfoDefensivelyCopiesConfiguredProducts() {
    DataProductConfig product = new DataProductConfig();
    product.setIdentifier("dp-rtsp-feed-001");
    product.setUri("rtsp://drone01/videofeed01");
    product.setProductType(Map.of("name", "video/rtsp"));
    product.setConformsTo(Map.of("name", "ONVIF Profile S"));

    DroneInfoDTO droneInfo = new DroneInfoDTO();
    droneInfo.setDataProducts(List.of(product));

    assertEquals(1, droneInfo.getDataProducts().size());
    assertEquals("rtsp://drone01/videofeed01", droneInfo.getDataProducts().get(0).getUri());
  }
}
