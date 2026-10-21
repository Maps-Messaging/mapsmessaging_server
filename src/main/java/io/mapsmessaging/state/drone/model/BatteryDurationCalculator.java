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

package io.mapsmessaging.state.drone.model;

public final class BatteryDurationCalculator {

  private static final double MILLIAMPS_PER_AMP = 1000.0;
  private static final double SECONDS_PER_HOUR = 3600.0;
  private static final long SECONDS_PER_DAY = 86_400;
  private static final long SECONDS_PER_HOUR_LONG = 3_600;
  private static final long SECONDS_PER_MINUTE = 60;

  private BatteryDurationCalculator() {
  }

  public static String calculateDuration(BatteryState batteryState) {
    if (batteryState == null) {
      return null;
    }

    return calculateDuration(
        batteryState.getRemainingMilliampHours(),
        batteryState.getCurrentAmps()
    );
  }

  public static String calculateDuration(Double remainingMilliampHours, Double currentAmps) {
    if (remainingMilliampHours == null || currentAmps == null || currentAmps <= 0.0) {
      return null;
    }

    if (!Double.isFinite(remainingMilliampHours) || !Double.isFinite(currentAmps)) {
      return null;
    }

    double remainingAmpHours = remainingMilliampHours / MILLIAMPS_PER_AMP;
    if (remainingAmpHours < 0.0) {
      return null;
    }

    long totalSeconds = Math.round((remainingAmpHours / currentAmps) * SECONDS_PER_HOUR);
    return toIso8601Duration(totalSeconds);
  }

  public static String calculateDurationFromAmpHours(Double remainingAmpHours, Double currentAmps) {
    if (remainingAmpHours == null || currentAmps == null || currentAmps <= 0.0) {
      return null;
    }

    if (!Double.isFinite(remainingAmpHours) || !Double.isFinite(currentAmps) || remainingAmpHours < 0.0) {
      return null;
    }

    long totalSeconds = Math.round((remainingAmpHours / currentAmps) * SECONDS_PER_HOUR);
    return toIso8601Duration(totalSeconds);
  }

  private static String toIso8601Duration(long totalSeconds) {
    if (totalSeconds <= 0) {
      return "PT0S";
    }

    long days = totalSeconds / SECONDS_PER_DAY;
    totalSeconds %= SECONDS_PER_DAY;

    long hours = totalSeconds / SECONDS_PER_HOUR_LONG;
    totalSeconds %= SECONDS_PER_HOUR_LONG;

    long minutes = totalSeconds / SECONDS_PER_MINUTE;
    long seconds = totalSeconds % SECONDS_PER_MINUTE;

    StringBuilder builder = new StringBuilder("P");

    if (days > 0) {
      builder.append(days).append("D");
    }

    if (hours > 0 || minutes > 0 || seconds > 0 || days == 0) {
      builder.append("T");

      if (hours > 0) {
        builder.append(hours).append("H");
      }
      if (minutes > 0) {
        builder.append(minutes).append("M");
      }
      if (seconds > 0 || (hours == 0 && minutes == 0)) {
        builder.append(seconds).append("S");
      }
    }

    return builder.toString();
  }
}