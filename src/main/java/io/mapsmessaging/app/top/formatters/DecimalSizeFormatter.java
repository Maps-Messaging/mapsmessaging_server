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

package io.mapsmessaging.app.top.formatters;

public class DecimalSizeFormatter implements Formatter {

  private static final long K = 1000;
  private static final long M = K * 1000;
  private static final long G = M * 1000;
  private static final long T = G * 1000;

  private final int len;
  private final boolean prepend;
  private final boolean withSpace;

  public DecimalSizeFormatter(int len) {
    this(len,false, true);
  }

  public DecimalSizeFormatter(int len, boolean prepend, boolean withSpace) {
    this.len = len;
    this.prepend = prepend;
    this.withSpace = withSpace;
  }

  @Override
  public String format(Object value) {
    if (value instanceof Number) {
      long val = ((Number) value).longValue();
      if(withSpace) {
        return pad(formatSize(val), len, prepend);
      }
      return pad(formatSizeNoSpace(val), len, prepend);
    }
    return null;
  }


  public static String formatSizeNoSpace(long bytes) {
    if (bytes >= T) {
      return String.format("%dT", (int) (bytes / (double) T));
    } else if (bytes >= G) {
      return String.format("%dG", (int) (bytes / (double) G));
    } else if (bytes >= M) {
      return String.format("%dM", (int) (bytes / (double) M));
    } else if (bytes >= K) {
      return String.format("%dK", (int) (bytes / (double) K));
    } else {
      return "" + bytes;
    }
  }
  public static String formatSize(long bytes) {
    if (bytes >= T) {
      return String.format("%d T", (int) (bytes / (double) T));
    } else if (bytes >= G) {
      return String.format("%d G", (int) (bytes / (double) G));
    } else if (bytes >= M) {
      return String.format("%d M", (int) (bytes / (double) M));
    } else if (bytes >= K) {
      return String.format("%d K", (int) (bytes / (double) K));
    } else {
      return "" + bytes;
    }
  }
}
