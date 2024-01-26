/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.utilities;

import com.fasterxml.uuid.Generators;

import java.util.UUID;

public class UuidGenerator {

  static {
    int val = 7;
    String version = System.getProperty("UUID_VERSION");
    if (version != null) {
      try {
        val = Integer.parseInt(version);
      } catch (Throwable th) {
        // ignore
      }
    }
    UUID_VERSION = val;
  }

  private static final int UUID_VERSION;

  public static UUID generate() {
    switch (UUID_VERSION) {
      case 1:
        return Generators.timeBasedGenerator().generate();
      case 4:
        return UUID.randomUUID();
      case 6:
        return Generators.timeBasedReorderedGenerator().generate();
      case 7:
      default:
        return Generators.timeBasedEpochGenerator().generate();
    }
  }

  private UuidGenerator() {
  }
}
