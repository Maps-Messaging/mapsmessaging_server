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
package io.mapsmessaging.location;

import lombok.Getter;

@SuppressWarnings("java:S6548") // yes it is a singleton
@Getter
public class LocationManager {

  private static class Holder {
    static final LocationManager INSTANCE = new LocationManager();
  }
  public static LocationManager getInstance() {
    return Holder.INSTANCE;
  }

  private boolean set;
  private double longitude;
  private double latitude;

  public synchronized void setPosition(double latitude, double longitude) {
    this.latitude = (latitude);
    this.longitude = (longitude);
    set = true;
  }

  private LocationManager() {
    longitude = Double.NaN;
    latitude = Double.NaN;
    set = false;
  }
}
