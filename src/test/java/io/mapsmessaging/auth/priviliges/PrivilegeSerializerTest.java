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

package io.mapsmessaging.auth.priviliges;

import org.junit.jupiter.api.Test;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrivilegeSerializerTest {

  private final PrivilegeSerializer serializer = new PrivilegeSerializer();

  @Test
  void roundTrip_preservesIdentityAndAllSupportedPrivilegeTypes() throws IOException {
    UUID uniqueId = UUID.fromString("f02e5952-d394-4b46-8712-05ce27fedf20");
    SessionPrivileges original = new SessionPrivileges(uniqueId, "test-user", List.of(
        new BooleanPrivilege("enabled", true),
        new LongPrivilege("maximum", Long.MAX_VALUE),
        new StringPrivilege("domain", "auth-domain")
    ));

    SessionPrivileges restored = roundTrip(original);

    assertEquals(original, restored);
  }

  @Test
  void roundTrip_preservesEmptyPrivilegeList() throws IOException {
    SessionPrivileges original = new SessionPrivileges(
        UUID.fromString("754c615a-1385-43f4-a3de-fca8a1414603"),
        "no-privileges",
        List.of()
    );

    SessionPrivileges restored = roundTrip(original);

    assertEquals(original, restored);
  }

  private SessionPrivileges roundTrip(SessionPrivileges privileges) throws IOException {
    DataOutput2 output = new DataOutput2();
    serializer.serialize(output, privileges);
    return serializer.deserialize(new DataInput2.ByteArray(output.copyBytes()), output.pos);
  }
}
