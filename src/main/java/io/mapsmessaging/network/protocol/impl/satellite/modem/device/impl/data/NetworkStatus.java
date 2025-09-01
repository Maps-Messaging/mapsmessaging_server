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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class NetworkStatus {
    private final boolean canSend;
    private final String reason;

    private NetworkStatus(boolean canSend, String reason) {
      this.canSend = canSend;
      this.reason = reason;
    }

    public static NetworkStatus parse(String line) {
      line = line == null ? "" : line.trim();
      if(line.startsWith("%NETINFO:")){
        line = line.substring("%NETINFO:".length()).trim();
      }
      if (line.contains(",")) { // OGx (%NETINFO)
        return parseOgx(line);
      }
      else if(line.equals("ERROR")){
        return new NetworkStatus(true, "Modem Emulator");
      } else { // IDP (ATS54)
        return parseAts54(line);
      }
    }

    public boolean canSend() {
      return canSend;
    }

    public String noSendReason() {
      return reason;
    }

    // --- Internals ---

    private static NetworkStatus parseAts54(String line) {
      int v = safeInt(line);
      return switch (v) {
        case 5 -> new NetworkStatus(true, null); // TX enabled
        case 0 -> new NetworkStatus(false, "unknown");
        case 1 -> new NetworkStatus(false, "stopped/failed");
        case 2 -> new NetworkStatus(false, "searching");
        case 3, 4 -> new NetworkStatus(false, "receive only");
        case 6 -> new NetworkStatus(false, "TX suspended (network)");
        case 7 -> new NetworkStatus(false, "TX muted (user)");
        case 8 -> new NetworkStatus(false, "TX blocked (no beam)");
        default -> new NetworkStatus(false, "invalid state");
      };
    }

    private static NetworkStatus parseOgx(String line) {
      // "regStatus,nwkStatus,txState,rxInProgress,txInProgress"
      int[] v = parseCsv5(line);
      int reg = v[0];
      int nwk = v[1];
      int tx = v[2];

      if (reg == 2 && (nwk == 5 || nwk == 6) && tx == 0) {
        return new NetworkStatus(true, null); // Connected + TX allowed
      }
      if (reg != 2) return new NetworkStatus(false, "not registered");
      if (nwk == 0) return new NetworkStatus(false, "network offline");
      if (nwk <= 2) return new NetworkStatus(false, "acquiring");
      if (nwk == 3) return new NetworkStatus(false, "downloading config");
      if (nwk == 4) return new NetworkStatus(false, "registered (RX only)");

      // nwk 5/6 but gated by txState
      return switch (tx) {
        case 1 -> new NetworkStatus(false, "network mute");
        case 2 -> new NetworkStatus(false, "user mute");
        case 3 -> new NetworkStatus(false, "GNSS position stale");
        case 4 -> new NetworkStatus(false, "satellite motion model");
        case 5 -> new NetworkStatus(false, "doppler velocity stale");
        default -> new NetworkStatus(false, "unknown");
      };
    }

    private static int[] parseCsv5(String line) {
      String[] parts = line.split(",", -1);
      int[] out = {-1, -1, -1, -1, -1};
      for (int i = 0; i < Math.min(5, parts.length); i++) {
        String t = parts[i].trim();
        int eq = t.indexOf('=');
        if (eq >= 0) t = t.substring(eq + 1).trim();
        out[i] = safeInt(t);
      }
      return out;
    }

    private static int safeInt(String s) {
      try { return Integer.parseInt(s.trim()); }
      catch (Exception e) { return -1; }
    }

  }
