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

package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ListOption;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Option;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.PathOption;
import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Queue;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.EMPTY;

@ToString
public class BasePacket implements ServerPacket {

  @Getter
  protected final int id;

  @Getter
  @Setter
  protected byte[] token;

  @Getter
  @Setter
  protected SocketAddress fromAddress;

  @Getter
  @Setter
  private Runnable callback;

  @Getter
  @Setter
  protected int version;

  @Getter
  @Setter
  protected Code code;

  @Getter
  @Setter
  protected TYPE type;

  @Getter
  @Setter
  protected int messageId;

  @Getter
  protected final OptionSet options;

  @Getter
  @Setter
  protected byte[] payload;

  @Getter
  @Setter
  private long timeSent;

  private int resentCount;

  public BasePacket(int id, TYPE type, Code code, int version, int messageId, byte[] token){
    this.id = id;
    this.type = type;
    this.code = code;
    this.token = token;
    this.version = version;
    this.messageId = messageId;
    options = new OptionSet();
  }

  public BasePacket(int id, Packet packet) {
    this.id = id;
    byte val = packet.get();
    version = (val >> 6 & 0b11);
    type = TYPE.valueOf((val >> 4) & 0b11);
    int tokenLength = (val) & 0b1111;

    val = packet.get();
    code = Code.valueOf(val);

    messageId = (packet.get() & 0xff) << 8;
    messageId += (packet.get() & 0xff);
    token = new byte[tokenLength];
    packet.get(token);

    options = new OptionSet();
  }


  public void sent(BasePacket response) {
    // this is only used when a single packet results in multiple response packets like a Block
  }

  public boolean isComplete() {
    return true;
  }


  public int incrementResendCount() {
    return resentCount++;
  }

  public BasePacket buildUpdatePacket(Code code) {
    TYPE responseType =type.equals(TYPE.CON) ? TYPE.CON : TYPE.NON;
    return new BasePacket(id,responseType, code, 1, messageId, token );
  }

  public BasePacket buildAckResponse(Code code) {
    TYPE responseType = type.equals(TYPE.CON) ? TYPE.ACK : TYPE.NON;
    BasePacket response = new BasePacket(id, responseType, code, 1, messageId, token);
    response.setFromAddress(getFromAddress());
    return response;
  }

  public @NotNull BasePacket buildWaitResponse() {
    TYPE responseType = type.equals(TYPE.CON) ? TYPE.ACK : TYPE.NON;
    BasePacket response = new BasePacket(EMPTY, responseType, Code.EMPTY, 1, messageId, new byte[0]);
    response.setFromAddress(getFromAddress());
    return response;
  }

  @Override
  public int packFrame(Packet packet) {
    int tokenLength = token != null ? token.length:0;
    packet.put((byte) ((version & 0b11) << 6 | ((type.getValue() & 0b11) << 4) | (tokenLength & 0b1111)));
    packet.put(code.getValue());
    packet.put((byte) (messageId >> 8 & 0xff));
    packet.put((byte) (messageId & 0xff));
    if(tokenLength > 0) packet.put(token);

    Queue<Long> optionKeys = new NaturalOrderedLongQueue();
    options.getOptionList().keySet().forEach(integer -> {
      long optionId = integer;
      optionKeys.add(optionId);
    });

    int currentOptionId = 0;
    int currentDelta;
    while(!optionKeys.isEmpty()){
      long optionId = optionKeys.remove();
      Option option = options.getOption((int) optionId);
      currentDelta = (int) optionId - currentOptionId;
      currentOptionId = (int) optionId;
      if(option instanceof PathOption){
        PathOption pathOption = (PathOption) option;
        for(String path:pathOption.getPath()){
          byte[] packed = path.getBytes();
          packOption(packet, currentDelta, packed);
          currentDelta = (int) optionId - currentOptionId;
          currentOptionId = (int) optionId;
        }
      }
      else if(option instanceof ListOption){
        ListOption listOption = (ListOption)option;
        for(byte[] opt:listOption.getList()){
          packOption(packet, currentDelta, opt);
          currentDelta = (int) optionId - currentOptionId;
          currentOptionId = (int) optionId;
        }
      } else {
        packOption(packet, currentDelta, option.pack());
      }
    }
    packPayload(packet);
    return packet.position();
  }

  private void packPayload(Packet packet) {
    if (payload != null && payload.length > 0) {
      packet.put((byte) 0xff);
      int available = packet.available();
      if (available >= payload.length) {
        packet.put(payload);
      } else {

        packet.put(payload, 0, available);
      }
    }
  }

  private void packOption(Packet packet, int optionId, byte[] data) {
    int optionIdSize = computeVariableValue(optionId);
    int optionSize = computeVariableValue(data.length);
    byte optionHeader = (byte) ((optionIdSize << 4) | (optionSize & 0xf));
    packet.put(optionHeader);
    writeVariableInt(packet, optionIdSize, optionId); // Write the option ID, if any to send
    writeVariableInt(packet, optionHeader, optionSize);      // Write the option size, if any to send
    packet.put(data);
  }

  @Override
  public void complete() {
    Runnable tmp;
    synchronized (this) {
      tmp = callback;
      callback = null;
    }
    if (tmp != null) {
      tmp.run();
    }
  }

  protected void readOptions(@NotNull Packet packet) throws IOException{
    int optionNumber = 0;
    while(packet.hasData()){
      int val = packet.get() & 0xff;
      if(val == 0xFF) return; // Found payload flag
      optionNumber += readVariableInt(packet, val >> 4);
      int optionLength = readVariableInt(packet, val & 0xf);
      byte[] data = new byte[optionLength];
      packet.get(data);
      Option option = options.getOption(optionNumber);
      option.update(data);
    }
  }

  private int computeVariableValue(int totalSize){
    if(totalSize <= 12){
      return totalSize;
    }
    else if (totalSize > (256+13)){
      return 13;
    }
    else return 14;
  }

  private void writeVariableInt(Packet packet, int nibble, long val) {
    if(nibble <= 12){
      return;
    }
    if(nibble == 13){
      packet.put((byte)((val -13) & 0xff));
    }
    else if(nibble == 14){
      packet.put((byte)( (val -13)&0xff));
      packet.put((byte)(val>>8 & 0xff));
    }
  }

  private int readVariableInt(Packet packet, int val) throws IOException {
    if (val <= 12) {
      return val;
    } else if (val == 13) {
      return (packet.get() & 0xff) + 13;
    } else if (val == 14) {
      val = (packet.get() & 0xff) << 8;
      val = val | (packet.get() & 0xff);
      return val + 269;
    } else {
      throw new IOException("Invalid variable int header found");
    }
  }

  public void readPayload(Packet packet) {
    // The 0xff has already been stripped
    int size = packet.available();
    payload = new byte[size];
    packet.get(payload);
  }
}
