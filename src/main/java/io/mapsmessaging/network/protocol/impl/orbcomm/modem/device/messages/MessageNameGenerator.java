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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages;

public class MessageNameGenerator {

  private MessageNameGenerator() {
  }

  public static String incrementString(String current) {
    long value = base36ToDecimal(current);
    return decimalToBase36(value + 1);
  }

  public static long base36ToDecimal(String input) {
    input = input.trim().toLowerCase();
    if (input.isEmpty()) return 0;

    long result = 0;
    for (char c : input.toCharArray()) {
      int digit;
      if (c >= '0' && c <= '9') {
        digit = c - '0';
      } else if (c >= 'a' && c <= 'z') {
        digit = c - 'a' + 10;
      } else {
        throw new IllegalArgumentException("Invalid char: " + c);
      }
      result = result * 36 + digit;
    }
    return result;
  }

  public static String decimalToBase36(long value) {
    if (value == 0) return "       0";

    StringBuilder sb = new StringBuilder();
    while (value > 0) {
      int rem = (int) (value % 36);
      char c = (rem < 10) ? (char) ('0' + rem) : (char) ('a' + rem - 10);
      sb.append(c);
      value /= 36;
    }
    return String.format("%8s", sb.reverse());
  }
}
