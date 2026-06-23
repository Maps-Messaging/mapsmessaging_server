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

package io.mapsmessaging.state.stanag;


import io.mapsmessaging.security.uuid.NamedVersions;
import io.mapsmessaging.state.drone.util.UuidGenerator;

import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

public class StanagUuidGenerator extends UuidGenerator {

  @Override
  public UUID generateUuid(String namespace) {
    try {
      return resolvePlatformUuid("EE",  UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), "stickleback",  namespace);
    } catch (NoSuchAlgorithmException e) {
      return super.generateUuid(namespace);
    }
  }


  private UUID resolvePlatformUuid(String countryCode2Char, UUID suppliedUuid, String platformName, String platformId) throws NoSuchAlgorithmException {
    String uuidSource = ("urn:nato:stanag:4817:"+countryCode2Char+":" + platformName + ":" + platformId).toUpperCase(Locale.ROOT);
    return io.mapsmessaging.security.uuid.UuidGenerator.getInstance().generate(NamedVersions.SHA1, suppliedUuid, uuidSource );
  }
}
