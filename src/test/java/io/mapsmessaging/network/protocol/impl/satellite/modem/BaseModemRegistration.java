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

package io.mapsmessaging.network.protocol.impl.satellite.modem;

import io.mapsmessaging.network.protocol.impl.satellite.ModemResponder;

public abstract class BaseModemRegistration {

  protected final ModemResponder modemResponder;

  public BaseModemRegistration(ModemResponder modemResponder) {
    this.modemResponder = modemResponder;
  }

  public void registerModem(){

    modemResponder.registerHandler("%GPS", at ->
        "%GPS: $GPGGA,224444.000,2142.0675,S,15914.7646,E,1,05,3.0,0.00,M,,,,0000*2E\r\n" +
            "\r\n" +
            "$GPRMC,224444.000,A,2142.0675,S,15914.7646,E,0.00,000.00,250825,,,A*71\r\n" +
            "\r\n" +
            "$GPGSV,1,1,03,1,45,060,50,2,45,120,50,3,45,180,50*72\r\n" +
            "\r\n" +
            "OK\r\n"
    );

    // AT%TRK=10,1  → OK
    modemResponder.registerHandler("%TRK=10,1", at -> "OK\r\n");

    modemResponder.registerHandler("S57", at ->
        "\r\n" +
            "005\r\n" +
            "\r\n" +
            "OK\r\n"
    );

    modemResponder.registerHandler("S56", at ->
        "001\r\n" +
            "\r\n" +
            "OK\r\n"
    );

    modemResponder.registerHandler("S85", at ->
        "00250\r\n" +
            "\r\n" +
            "OK\r\n"
    );
    registerInit();
    registerSend();
    registerReceive();
  }

  protected abstract void registerInit();

  protected abstract void registerSend();

  protected abstract void registerReceive();

  protected int step(int length) {
    // ceil(20% of length), at least 1 byte to ensure progress
    int s = (int) Math.ceil(length * 0.2);
    return Math.max(1, s);
  }

  protected int safeParseInt(String s, int def) {
    try { return Integer.parseInt(s); } catch (Exception e) { return def; }
  }
}
