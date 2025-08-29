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

package io.mapsmessaging.network.protocol.impl.satellite.ogx;

import io.mapsmessaging.network.protocol.impl.satellite.idp.MoEntry;
import io.mapsmessaging.network.protocol.impl.satellite.ModemResponder;

import java.util.Base64;

import static io.mapsmessaging.network.protocol.impl.satellite.idp.MoEntry.*;

public class OgxModemRegistation {
  private static final java.time.format.DateTimeFormatter OGX_UTC_FMT =
      java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
          .withZone(java.time.ZoneOffset.UTC);

  private final ModemResponder modemResponder;
  public OgxModemRegistation(ModemResponder modemResponder) {
    this.modemResponder = modemResponder;
  }


  public void registerIdpModem(){
    modemResponder.registerHandler("E0;&W;I5", at -> "\r\n10\r\nOK");
    modemResponder.registerHandler("I0;+GMM;+GMR;+GMR;+GMI", at ->
        "ORBCOMM Inc\r\n" +
            "\r\n" +
            "+GMM: IsatDataPro Modem Simulator\r\n" +
            "\r\n" +
            "+GMR: 5.003,2.0,10\r\n" +
            "\r\n" +
            "+GMR: 5.003,2.0,10\r\n" +
            "\r\n" +
            "+GMI: ORBCOMM Inc\r\n" +
            "\r\n" +
            "OK\r\n"
    );
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

    // AT%NETINFO  → %NETINFO: 2,6,0,0,0  + blank line + OK
    modemResponder.registerHandler("%NETINFO", at ->
        "%NETINFO: 2,6,0,0,0 \r\n" +
            "\r\n" +
            "OK\r\n"
    );

    // AT%MTQS  → %MTQS:  (empty) + blank line + OK
    modemResponder.registerHandler("%MTQS", at ->
        "%MTQS: \r\n" +
            "\r\n" +
            "OK\r\n"
    );

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
    registerOgxMomt();
    registerOgxMgrl();
  }

  private void registerOgxMomt() {
    modemResponder.registerHandler("%MOMT", at -> {
      String params = at.getParams();                  // "0,2,10,352,3,gQE..."
      if (params != null && !params.isEmpty()) {
        String[] parts = params.split(",", 6);         // keep last field intact
        if (parts.length == 6) {
          String messageId = parts[0].trim();
          int length = safeParseInt(parts[3].trim(), 0);
          String b64 = parts[5].trim();
          try {
            byte[] payload = Base64.getDecoder().decode(b64);
            modemResponder.getOutgoingMessages().add(payload);
            if (length > 0 && !messageId.isEmpty()) {
              modemResponder.getOustandingEntries().add(new OgxMoEntry(safeParseInt(messageId, 1), 2, 10, length));
            }
          } catch (IllegalArgumentException ignore) {
            // bad base64; ignore or log in your test harness if needed
          }
        }
      }
      return "OK\r\n";
    });
  }

  private void registerOgxMgrl() {
    modemResponder.registerHandler("%MOQS", at -> {
      StringBuilder resp = new StringBuilder();
      java.util.concurrent.ConcurrentLinkedQueue<OgxMoEntry> next = new java.util.concurrent.ConcurrentLinkedQueue<>();

      for (MoEntry entry; (entry = modemResponder.getOustandingEntries().poll()) != null; ) {
        OgxMoEntry e = (OgxMoEntry) entry;
        // timestamp first seen
        if (e.getUtcFirstSeen() == null) {
          e.setUtcFirstSeen(OGX_UTC_FMT.format(java.time.Instant.now()));
        }

        // advance state/acks
        if (e.getState() == TX_SENDING) {
          e.setBytesAck( Math.min(e.getLength(), e.getBytesAck() + step(e.getLength())));
          if (e.getBytesAck() >= e.getLength()) {
            e.setBytesAck(e.getLength());
            e.setState( OGX_TX_COMPLETED);
            e.setClosed(1);
            e.setCompletedEmittedOnce(false); // emit once as completed, then drop on next call
          }
        } else if (e.getState() == OGX_TX_COMPLETED) {
          if (e.isCompletedEmittedOnce()) {
            // drop entry (do not re-add)
            continue;
          }
        }

        // emit one line
        resp.append("%MOQS: ")
            .append(e.getType()).append(",")
            .append(e.getMessageId()).append(",")
            .append(e.getUtcFirstSeen()).append(",")
            .append(e.getState()).append(",")
            .append(e.getClosed()).append(",")
            .append(e.getServiceClass()).append(",")
            .append(e.getLifetimeMins()).append(",")
            .append(e.getLength()).append(",")
            .append(e.getBytesAck())
            .append("\r\n");

        // keep for next round if not fully dropped
        if (e.getState() == OGX_TX_COMPLETED) {
          e.setCompletedEmittedOnce(true);
          next.add(e);
        } else {
          next.add(e);
        }
      }

      // swap queues
      modemResponder.getOustandingEntries().addAll(next);

      resp.append("OK\r\n");
      return resp.toString();
    });
  }

  private static int step(int length) {
    // ceil(20% of length), at least 1 byte to ensure progress
    int s = (int) Math.ceil(length * 0.2);
    return Math.max(1, s);
  }

  private static int safeParseInt(String s, int def) {
    try { return Integer.parseInt(s); } catch (Exception e) { return def; }
  }
}
