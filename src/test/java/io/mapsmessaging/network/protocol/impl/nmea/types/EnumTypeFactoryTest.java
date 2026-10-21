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

package io.mapsmessaging.network.protocol.impl.nmea.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumTypeFactoryTest {

  @Test
  void registeredEnumValuesCanBeResolvedById() {
    String name = "test-enum-" + getClass().getSimpleName();
    EnumTypeFactory factory = EnumTypeFactory.getInstance();

    factory.register(name, "[{\"0\":\"Off\"},{\"1\":\"On\"},{\"2\":\"Auto\"}]");

    EnumType off = factory.getEnum(name, "0");
    EnumType auto = factory.getEnum(name, "2");

    assertEquals("0", off.getId());
    assertEquals("Off", off.getDescription());
    assertEquals("0", off.toString());
    assertSame(off, off.jsonPack());

    assertEquals("2", auto.getId());
    assertEquals("Auto", auto.getDescription());
  }

  @Test
  void unknownEnumNameReturnsNullAndUnknownIdRetainsNullDescription() {
    EnumTypeFactory factory = EnumTypeFactory.getInstance();
    String name = "unknown-id-" + getClass().getSimpleName();
    factory.register(name, "[{\"1\":\"One\"}]");

    assertNull(factory.getEnum("not-registered-" + getClass().getSimpleName(), "1"));

    EnumType value = factory.getEnum(name, "99");
    assertNotNull(value);
    assertEquals("99", value.getId());
    assertNull(value.getDescription());
  }
}
