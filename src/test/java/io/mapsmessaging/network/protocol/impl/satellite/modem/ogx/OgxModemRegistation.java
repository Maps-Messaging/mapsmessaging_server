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

package io.mapsmessaging.network.protocol.impl.satellite.modem.ogx;

import io.mapsmessaging.network.protocol.impl.satellite.modem.BaseModemRegistration;
import io.mapsmessaging.network.protocol.impl.satellite.modem.SentMessageEntry;
import io.mapsmessaging.network.protocol.impl.satellite.modem.ModemResponder;

import java.util.Base64;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mapsmessaging.network.protocol.impl.satellite.modem.ogx.OgxSentMessageEntry.TX_COMPLETED;
import static io.mapsmessaging.network.protocol.impl.satellite.modem.ogx.OgxSentMessageEntry.TX_SENDING;

public class OgxModemRegistation extends BaseModemRegistration {

  public static final java.time.format.DateTimeFormatter OGX_UTC_FMT =
      java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
          .withZone(java.time.ZoneOffset.UTC);

  private final ConcurrentLinkedQueue<OgxEntry> ogxInbox = new ConcurrentLinkedQueue<>();
  private final AtomicInteger ogxMsgSeq = new AtomicInteger(0);


  public OgxModemRegistation(ModemResponder modemResponder) {
    super(modemResponder);
  }

  @Override
  protected void registerInit() {
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
    modemResponder.registerHandler("%NETINFO", at ->
        "%NETINFO: 2,6,0,0,0 \r\n" +
            "\r\n" +
            "OK\r\n"
    );

  }

  @Override
  protected void registerReceive() {

    modemResponder.registerHandler("%MTQS", at -> {
      for (byte[] p; (p = modemResponder.getIncomingMessages().poll()) != null; ) {
        int num = ogxMsgSeq.getAndIncrement();

        // Create entry with metadata + payload
        ogxInbox.add(new OgxEntry(num, p));
      }

      StringBuilder out = new StringBuilder();
      for (OgxEntry e : ogxInbox) {
        out.append("%MTQS: ")
            .append(e.type).append(",")
            .append(e.msgId).append(",")
            .append(e.timestamp).append(",")
            .append(e.state).append(",")
            .append(e.closed).append(",")
            .append(e.length).append("\r\n");
      }
      out.append("\r\nOK\r\n");
      return out.toString();
    });

    modemResponder.registerHandler("%MTMG", at -> {
      String p = at.getParams();           // "123,3"
      if (p == null || p.isBlank()) return "ERROR\r\n";
      String[] parts = p.split(",", 2);
      int msgId = parseIntOr(parts[0], -1);
      int fmt = (parts.length > 1) ? parseIntOr(parts[1], 3) : 3;
      if (msgId < 0 || fmt != 3) return "ERROR\r\n";

      OgxEntry hit = null;
      for (OgxEntry e : ogxInbox) { if (e.msgId == msgId) { hit = e; break; } }
      if (hit == null) return "\r\nOK\r\n";

      String b64 = java.util.Base64.getEncoder().encodeToString(hit.data);
      String resp = "%MTMG: " + hit.msgId + "," + hit.timestamp + "," + hit.length + ",3," + b64 + "\r\n\r\nOK\r\n";
      return resp;
    });

    modemResponder.registerHandler("%MTMD", at -> {
      String p = at.getParams(); // "123"
      if (p == null || p.isBlank()) return "ERROR\r\n";
      int msgId = parseIntOr(p, -1);
      if (msgId < 0) return "ERROR\r\n";
      boolean removed = ogxInbox.removeIf(e -> e.msgId == msgId);
      return removed ? "OK\r\n" : "ERROR\r\n";
    });
  }

  protected void registerSend() {
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
              modemResponder.getOustandingEntries().add(new OgxSentMessageEntry(messageId, 2, 10, length));
            }
          } catch (IllegalArgumentException ignore) {
            // bad base64; ignore or log in your test harness if needed
          }
        }
      }
      return "OK\r\n";
    });

    modemResponder.registerHandler("%MOQS", at -> {
      StringBuilder resp = new StringBuilder();
      java.util.concurrent.ConcurrentLinkedQueue<OgxSentMessageEntry> next = new java.util.concurrent.ConcurrentLinkedQueue<>();

      for (SentMessageEntry entry; (entry = modemResponder.getOustandingEntries().poll()) != null; ) {
        OgxSentMessageEntry e = (OgxSentMessageEntry) entry;
        // timestamp first seen
        if (e.getUtcFirstSeen() == null) {
          e.setUtcFirstSeen(OGX_UTC_FMT.format(java.time.Instant.now()));
        }

        // advance state/acks
        if (e.getState() == TX_SENDING) {
          e.setBytesAck( Math.min(e.getLength(), e.getBytesAck() + step(e.getLength())));
          if (e.getBytesAck() >= e.getLength()) {
            e.setBytesAck(e.getLength());
            e.setState( TX_COMPLETED);
            e.setClosed(1);
            e.setCompletedEmittedOnce(false); // emit once as completed, then drop on next call
          }
        } else if (e.getState() == TX_COMPLETED) {
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
        if (e.getState() == TX_COMPLETED) {
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

  private static int parseIntOr(String s, int def) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }

}
