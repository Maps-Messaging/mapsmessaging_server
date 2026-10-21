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

package io.mapsmessaging.license;

import io.mapsmessaging.license.features.Engine;
import io.mapsmessaging.license.features.Features;
import io.mapsmessaging.license.features.Protocols;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeatureManagerTest {

  @Test
  void isEnabled_resolvesNestedFeaturePaths_acrossLicenses() {
    Features community = features("community");
    community.setProtocols(new Protocols());

    Features enterprise = features("enterprise");
    Protocols enterpriseProtocols = new Protocols();
    enterpriseProtocols.setMqtt(true);
    enterprise.setProtocols(enterpriseProtocols);

    FeatureManager manager = new FeatureManager(List.of(details(community, "base"), details(enterprise, "full")));

    assertTrue(manager.isEnabled("protocols.mqtt"));
    assertFalse(manager.isEnabled("protocols.amqp"));
  }

  @Test
  void isEnabled_unknownOrNullNestedPath_returnsFalse() {
    Features features = features("community");
    FeatureManager manager = new FeatureManager(List.of(details(features, "base")));

    assertFalse(manager.isEnabled("protocols.mqtt"));
    assertFalse(manager.isEnabled("protocols.unknown"));
    assertFalse(manager.isEnabled("unknown"));
  }

  @Test
  void isEnabled_overrideEnablesFeaturesExceptMl() {
    Features features = features("override");
    features.setOverrideFeatures(true);
    FeatureManager manager = new FeatureManager(List.of(details(features, "override")));

    assertTrue(manager.isEnabled("protocols.mqtt"));
    assertFalse(manager.isEnabled("ml"));
  }

  @Test
  void getMaxValue_returnsLargestConfiguredValue_andZeroForUnknownPath() {
    Features smaller = features("small");
    Engine smallerEngine = new Engine();
    smallerEngine.setMaxTopics(10);
    smaller.setEngine(smallerEngine);

    Features larger = features("large");
    Engine largerEngine = new Engine();
    largerEngine.setMaxTopics(250);
    larger.setEngine(largerEngine);

    FeatureManager manager = new FeatureManager(List.of(details(smaller, "small"), details(larger, "large")));

    assertEquals(250, manager.getMaxValue("engine.maxTopics"));
    assertEquals(0, manager.getMaxValue("engine.unknown"));
    assertEquals(0, manager.getMaxValue("protocols.maxTopics"));
  }

  @Test
  void loadAndDescriptions_preserveLicenseOrder() {
    FeatureDetails first = details(features("community"), "base");
    FeatureDetails second = details(features("enterprise"), "full");
    FeatureManager manager = new FeatureManager(List.of(first, second));

    assertSame(first, manager.loadLicense());
    assertEquals("community, enterprise, ", manager.getLoadedLicenses());
    assertEquals("base, full, ", manager.getLoadedInfo());
  }

  private Features features(String name) {
    Features features = new Features();
    features.setName(name);
    return features;
  }

  private FeatureDetails details(Features features, String info) {
    FeatureDetails details = new FeatureDetails();
    details.setFeature(features);
    details.setInfo(info);
    return details;
  }
}
