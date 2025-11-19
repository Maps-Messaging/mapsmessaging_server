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

package io.mapsmessaging.network.protocol.impl.nats.frames;

import com.google.gson.Gson;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.utilities.GsonFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
@ToString
public class InfoFrame extends NatsFrame {

  private static final AtomicLong counter = new AtomicLong();

  private InfoData infoData;

  public InfoFrame(int maxPayloadLength) {
    super();
    this.infoData = new InfoData();
    this.infoData.setClientId(counter.incrementAndGet());
    this.infoData.setMaxPayloadLength(maxPayloadLength);
    infoData.setVersion("2.11.3");
    infoData.setProto(1);
    infoData.setHost("localhost");
    infoData.setPort(4222);
    infoData.setHeaders(true);
    infoData.setClientIp("127.0.0.1");
    infoData.setServerId(MessageDaemon.getInstance().getUuid().toString());
    infoData.setServerName(MessageDaemon.getInstance().getId());
    infoData.setJava(System.getProperty("java.version"));
  }

  @Override
  public byte[] getCommand() {
    return "INFO".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String json) {
    this.infoData = GsonFactory.getInstance().getSimpleGson().fromJson(json, InfoData.class);
  }

  @Override
  public int packFrame(Packet packet) {
    int start = packet.position();
    packet.put(getCommand());
    packet.put((byte) ' ');
    String json = GsonFactory.getInstance().getSimpleGson().toJson(infoData);
    packet.put(json.getBytes(StandardCharsets.US_ASCII));
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));
    return packet.position() - start;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public NatsFrame instance() {
    return new InfoFrame(infoData.getMaxPayloadLength());
  }
}

