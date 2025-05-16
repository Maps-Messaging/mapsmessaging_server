/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.nmea;

public class Constants {

  public static final char CR = 0xd;
  public static final char LF = 0xa;
  public static final char ENCAPSULATION = 0x21;
  public static final char START = 0x24;
  public static final char CHECKSUM = 0x2a;
  public static final char FIELD = 0x2c;
  public static final char TAG = 0x5c;
  public static final char CODE = 0x5e;

  private Constants() {
    // nothing to do
  }
}
