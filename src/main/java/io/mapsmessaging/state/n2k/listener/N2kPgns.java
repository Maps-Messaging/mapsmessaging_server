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

package io.mapsmessaging.state.n2k.listener;

public final class N2kPgns {

  public static final int ISO_REQUEST = 59904;
  public static final int ISO_ADDRESS_CLAIM = 60928;

  public static final int SYSTEM_TIME = 126992;
  public static final int HEARTBEAT = 126993;

  public static final int VESSEL_HEADING = 127250;
  public static final int RATE_OF_TURN = 127251;
  public static final int ATTITUDE = 127257;
  public static final int MAGNETIC_VARIATION = 127258;

  public static final int BATTERY_STATUS = 127508;
  public static final int INVERTER_STATUS = 127509;

  public static final int POSITION_RAPID_UPDATE = 129025;
  public static final int COG_SOG_RAPID_UPDATE = 129026;
  public static final int TIME_DATE = 129033;
  public static final int GNSS_POSITION_DATA = 129029;
  public static final int GNSS_DOPS = 129539;

  public static final int AIS_CLASS_A_POSITION_REPORT = 129038;
  public static final int AIS_CLASS_B_POSITION_REPORT = 129039;

  public static final int WIND_DATA = 130306;
  public static final int ENVIRONMENTAL_PARAMETERS = 130311;

  private N2kPgns() {
  }
}