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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages;

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;
import io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem.Xmodem;
import lombok.Data;
import lombok.ToString;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@ToString
@Data
public class ModemSatelliteMessage {
  private static final AtomicInteger nextId = new AtomicInteger(0);
  private static final int OGX_SERVICE_CLASS = 2;

  private String name;
  private byte[] payload;
  private int sin;
  private int min;
  private int priority;
  private MessageFormat format;
  private int messageId;
  private String datetime;
  private int lifeTime;

  public ModemSatelliteMessage() {
    lifeTime = 10;
    messageId = getNextId();
  }

  public ModemSatelliteMessage(String line, boolean isOgx) {
    if (line.startsWith("%MGFS:") || line.startsWith("%MTMG:")) {
      line = line.substring(6);
    }

    String data;
    String[] parts = line.split(",");
    if (isOgx) {
      name = parts[0];
      datetime = parts[1];
      format = MessageFormat.fromCode(Integer.parseInt(parts[parts.length - 2]));
      data = parts[parts.length - 1];
      if (format.equals(MessageFormat.TEXT)) {
        min = Integer.parseInt(parts[3]) & 0xff;
        sin = Integer.parseInt(parts[4]) & 0xff;
      }
    } else {
      name = parts[0];
      sin = (Integer.parseInt(parts[3]) & 0xff);
      priority = Integer.parseInt(parts[4]);
      format = MessageFormat.fromCode(Integer.parseInt(parts[6]));
      data = parts[7];
    }

    this.payload = (data.isEmpty()) ? new byte[0] : this.format.decode(data);
    if (!format.equals(MessageFormat.TEXT) && isOgx) {
      sin = (payload[0] & 0xff);
      min = (payload[1] & 0xff);
      payload = Arrays.copyOfRange(payload, 2, payload.length);
    }
    if (!isOgx) {
      min = payload[0];
      payload = Arrays.copyOfRange(payload, 1, payload.length);
    }
  }

  private int getNextId() {
    return nextId.getAndIncrement() % 999;
  }

  public String toATCommand() {
    MessageFormat outboundFormat = requireFormat();
    String encodedPayload = outboundFormat.encode(requirePayload());
    return String.format("\"%s\",%d,%d.%d,%d,%s", name, priority, sin, min, outboundFormat.getCode(), encodedPayload);
  }

  public String toOgxCommand() {
    byte[] outboundPayload = requirePayload();
    MessageFormat outboundFormat = requireFormat();
    byte[] extended = new byte[outboundPayload.length + 2];
    extended[0] = (byte) sin;
    extended[1] = (byte) min;
    System.arraycopy(outboundPayload, 0, extended, 2, outboundPayload.length);
    String encodedPayload = outboundFormat.encode(extended);
    return String.format("%d,%d,%d,%d,%d,%s", messageId, OGX_SERVICE_CLASS, lifeTime, extended.length, outboundFormat.getCode(), encodedPayload);
  }

  public XmodemData toOgxXModemCommand() {
    byte[] outboundPayload = requirePayload();
    byte[] extended = new byte[outboundPayload.length + 2];
    extended[0] = (byte) sin;
    extended[1] = (byte) min;
    System.arraycopy(outboundPayload, 0, extended, 2, outboundPayload.length);
    long crc = Xmodem.crc32Mpeg2(extended, extended.length) & 0xffffffffL;
    XmodemData data = new XmodemData();
    data.setCrc(crc);
    data.setData(extended);
    data.command = String.format("%d,%d,%d,%d,%d,%08X", messageId, OGX_SERVICE_CLASS, lifeTime, extended.length, 0, crc);
    return data;
  }

  private byte[] requirePayload() {
    byte[] outboundPayload = payload;
    if (outboundPayload == null) {
      throw new IllegalStateException("Satellite message payload is required");
    }
    return outboundPayload;
  }

  // Incomplete outbound messages must fail explicitly; production sendMessage sets the format.
  @SuppressWarnings("javabugs:S6416")
  private MessageFormat requireFormat() {
    MessageFormat outboundFormat = format;
    if (outboundFormat == null) {
      throw new IllegalStateException("Satellite message format is required");
    }
    return outboundFormat;
  }

  @Data
  public static class XmodemData{
    String command;
    byte[] data;
    long crc;
  }
}

