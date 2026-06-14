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

package io.mapsmessaging.engine.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaLocationHelperTest {

  @Test
  void compareVersionStrings_ordersNullVersionsBeforePresentVersions() {
    assertEquals(0, SchemaLocationHelper.compareVersionStrings(null, null));
    assertTrue(SchemaLocationHelper.compareVersionStrings(null, "1.0.0") < 0);
    assertTrue(SchemaLocationHelper.compareVersionStrings("1.0.0", null) > 0);
  }

  @Test
  void compareVersionStrings_ignoresPrefixWhitespaceCaseAndBuildMetadata() {
    assertEquals(0, SchemaLocationHelper.compareVersionStrings(" V1.2.3+build.7 ", "1.2.3+other"));
  }

  @Test
  void compareVersionStrings_comparesNumericPartsNumerically() {
    assertTrue(SchemaLocationHelper.compareVersionStrings("1.10.0", "1.2.0") > 0);
    assertEquals(0, SchemaLocationHelper.compareVersionStrings("2.1", "2.1.0"));
  }

  @Test
  void compareVersionStrings_ordersReleaseAfterPrerelease() {
    assertTrue(SchemaLocationHelper.compareVersionStrings("3.0.0", "3.0.0-rc") > 0);
    assertTrue(SchemaLocationHelper.compareVersionStrings("3.0.0-beta", "3.0.0-alpha") > 0);
  }
}
