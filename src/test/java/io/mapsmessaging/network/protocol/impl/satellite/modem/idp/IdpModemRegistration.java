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

package io.mapsmessaging.network.protocol.impl.satellite.modem.idp;

import io.mapsmessaging.network.protocol.impl.satellite.modem.ModemResponder;
import io.mapsmessaging.network.protocol.impl.satellite.modem.BaseModemRegistration;
import io.mapsmessaging.network.protocol.impl.satellite.modem.SentMessageEntry;

import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mapsmessaging.network.protocol.impl.satellite.modem.idp.Constants.*;

public class IdpModemRegistration extends BaseModemRegistration {
  private final ConcurrentLinkedQueue<IdpFwdEntry> idpIncomingQueue = new ConcurrentLinkedQueue<>();
  private final AtomicInteger idpMsgSeq = new AtomicInteger(0);

  public IdpModemRegistration(ModemResponder modemResponder) {
    super(modemResponder);
  }

  @Override
  protected void registerInit() {
    modemResponder.registerHandler("E0;&W;I5", at -> "\r\n8\r\nOK");
    modemResponder.registerHandler("I0;+GMM;+GMR;+GMR;+GMI", at ->
        "ORBCOMM Inc\n" +
            "\n" +
            "+GMM: IsatDataPro Modem Simulator\n" +
            "\n" +
            "+GMR: 4.002,2.0,8\n" +
            "\n" +
            "+GMR: 4.002,2.0,8\n" +
            "\n" +
            "+GMI: ORBCOMM Inc\n" +
            "\n" +
            "OK\n"
    );
    modemResponder.registerHandler("S54", at ->
        "005 \r\n" +
            "\r\n" +
            "OK\r\n"
    );
  }

  @Override
  protected void registerSend() {
    modemResponder.registerHandler("%MGRT", at -> {
      String params = at.getParams();                  // "0,2,10,352,3,gQE..."
      if (params != null && !params.isEmpty()) {
        String[] parts = params.split(",", 5);         // keep last field intact
        if (parts.length == 5) {
          String messageId = parts[0].trim();
          String b64 = parts[4].trim();
          try {
            byte[] payload = Base64.getDecoder().decode(b64);
            byte[] extended= new byte[payload.length+2];
            extended[0] = (byte) 128;
            extended[1] = (byte) 2;
            System.arraycopy(payload, 0, extended, 2, payload.length);
            modemResponder.getOutgoingMessages().add(extended);
            modemResponder.getOustandingEntries().add(new IdpSentMessageEntry(messageId,  extended.length));
          } catch (IllegalArgumentException ignore) {
            // bad base64; ignore or log in your test harness if needed
          }
        }
      }
      return "OK\r\n";
    });
    modemResponder.registerHandler("%MGRL", at -> {
      StringBuilder resp = new StringBuilder("%MGRL: ");
      ConcurrentLinkedQueue<SentMessageEntry> next = new ConcurrentLinkedQueue<>();

      for (SentMessageEntry entry; (entry = modemResponder.getOustandingEntries().poll()) != null; ) {
        IdpSentMessageEntry e = (IdpSentMessageEntry) entry;


        // advance state/acks
        if (e.getState() == TX_SENDING) {
          e.setBytesAck( Math.min(e.getLength(), e.getBytesAck() + step(e.getLength())));
          if (e.getBytesAck() >= e.getLength()) {
            e.setBytesAck(e.getLength());
            e.setState( TX_COMPLETED);
          }
        }
        if(e.getState() == TX_READY){
          e.setState(TX_SENDING);
        }
        // emit one line
        resp.append(e.getMessageId()).append(",")
            .append("0.0").append(",")
            .append("2").append(",")
            .append("129").append(",")
            .append(e.getState()).append(",")
            .append(e.getLength()).append(",")
            .append(e.getBytesAck())
            .append("\r\n");

        // keep for next round if not fully dropped
        next.add(e);
      }

      // swap queues
      modemResponder.getOustandingEntries().addAll(next);

      resp.append("\r\nOK\r\n");
      return resp.toString();
    });
    modemResponder.registerHandler("%MGRD", at -> {
      String msgId = at.getParams();
      StringBuilder resp = new StringBuilder();
      // supported in ConcurrentLinkedQueue
      modemResponder.getOustandingEntries().removeIf(sentMessageEntry -> msgId.equals(sentMessageEntry.getMessageId()));
      resp.append("OK\r\n");
      return resp.toString();
    });
  }

  @Override
  protected void registerReceive() {
    modemResponder.registerHandler("%MGFN", at -> {
      // Move any newly arrived payloads into listing entries (no state changes)
      for (byte[] p; (p = modemResponder.getIncomingMessages().poll()) != null; ) {
        int num = idpMsgSeq.getAndIncrement();
        byte[] msg = new byte[p.length -1];
        System.arraycopy(p, 1, msg, 0, msg.length);
        idpIncomingQueue.add(new IdpFwdEntry(num, "fwd-" + num, 4, 128, msg));
      }

      StringBuilder out = new StringBuilder();
      for (IdpFwdEntry e : idpIncomingQueue) {
        // %MGFN: "fwdMsgName", msgNum, priority, sin, state, length, bytesRxd
        out.append("%MGFN: ")
            .append('"').append(e.getName()).append('"').append(",")
            .append(e.getMsgNum()).append(",")
            .append(e.getPriority()).append(",")
            .append(e.getSin()).append(",")
            .append(e.getState()).append(",")
            .append(e.getLength()).append(",")
            .append(0)                             // bytesRxd fixed 0 until read/delete
            .append("\r\n");
      }
      out.append("\r\nOK\r\n");
      return out.toString();
    });

    modemResponder.registerHandler("%MGFG", at -> {
      String params = at.getParams(); // e.g. "\"fwd-0\",3"
      if (params == null || params.isBlank()) {
        return "ERROR\r\n";
      }

      // Split once on comma
      String[] parts = params.split(",", 2);
      String rawName = parts[0].trim();
      // Strip surrounding quotes if present
      if (rawName.startsWith("\"") && rawName.endsWith("\"")) {
        rawName = rawName.substring(1, rawName.length() - 1);
      }

      // Find the entry
      IdpFwdEntry hit = null;
      for (Iterator<IdpFwdEntry> it = idpIncomingQueue.iterator(); it.hasNext();) {
        IdpFwdEntry e = it.next();
        if (e.getName().equals(rawName)) {
          hit = e;
          it.remove(); // remove on fetch
          break;
        }
      }
      if (hit == null) {
        return "\r\nERROR\r\n"; // no such message
      }
      hit.setState(RX_RETRIEVED);
      String b64 = java.util.Base64.getEncoder().encodeToString(hit.getData());
      StringBuilder out = new StringBuilder();
      out.append("%MGFG: ")
          .append('"').append(hit.getName()).append('"').append(",")
          .append(hit.getMsgNum()).append(",")
          .append(hit.getPriority()).append(",")
          .append(hit.getSin()).append(",")
          .append(hit.getState()).append(",")
          .append(hit.getLength()).append(",")
          .append(3).append(",")
          .append(b64)
          .append("\r\n\r\nOK\r\n");

      return out.toString();
    });

  }
}
