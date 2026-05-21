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

package io.mapsmessaging.state.drone.util;


import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public final class SyntheticMmsiGenerator {

  /**
   * Internal synthetic prefix.
   *
   * <p>This is not an officially assigned MMSI range. It is only for use inside a private
   * environment where a stable 9-digit AIS identity is needed for simulated or synthetic targets.
   */
  private static final long SYNTHETIC_PREFIX = 980000000L;

  private static final long SUFFIX_MODULUS = 10_000_000L;

  private SyntheticMmsiGenerator() {
  }

  /**
   * Generates a stable synthetic 9-digit MMSI from the supplied twin id.
   *
   * <p>The same twin id will always produce the same MMSI.
   *
   * @param twinId the stable twin identifier
   * @return a 9-digit synthetic MMSI
   */
  public static long generateSyntheticMmsi(String twinId) {
    if (twinId == null || twinId.isBlank()) {
      throw new IllegalArgumentException("twinId must not be null or blank");
    }

    CRC32 crc32 = new CRC32();
    byte[] bytes = twinId.trim().getBytes(StandardCharsets.UTF_8);
    crc32.update(bytes, 0, bytes.length);

    long suffix = crc32.getValue() % SUFFIX_MODULUS;

    if (suffix == 0) {
      suffix = 1;
    }

    return SYNTHETIC_PREFIX + suffix;
  }

  /**
   * Returns the MMSI as a zero-padded 9 digit string.
   *
   * @param mmsi the numeric MMSI
   * @return the MMSI formatted as 9 digits
   */
  public static String formatMmsi(long mmsi) {
    if (mmsi < 100_000_000L || mmsi > 999_999_999L) {
      throw new IllegalArgumentException("MMSI must be a 9 digit number");
    }

    return String.format("%09d", mmsi);
  }
}