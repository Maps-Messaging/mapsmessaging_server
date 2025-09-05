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

public class ByteSizeFormatter implements Formatter {

  private static final long KB = 1024L;
  private static final long MB = KB * 1024;
  private static final long GB = MB * 1024;
  private static final long TB = GB * 1024;

  private final int len;
  private final boolean prepend;

  public ByteSizeFormatter(int len) {
    this(len, false);
  }

  public ByteSizeFormatter(int len, boolean prepend) {
    this.len = len;
    this.prepend = prepend;
  }

  @Override
  public String format(Object value) {
    if (value instanceof Number) {
      long val = ((Number) value).longValue();
      return pad(formatSize(val), len, prepend);
    }
    return null;
  }

  public static String formatSize(long bytes) {
    if (bytes >= TB) {
      return String.format("%.1f TB", bytes / (double) TB);
    } else if (bytes >= GB) {
      return String.format("%.1f GB", bytes / (double) GB);
    } else if (bytes >= MB) {
      return String.format("%.1f MB", bytes / (double) MB);
    } else if (bytes >= KB) {
      return String.format("%.1f KB", bytes / (double) KB);
    } else {
      return "" + bytes;
    }
  }
}
