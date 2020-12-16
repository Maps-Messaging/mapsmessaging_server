/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp.frames;

import java.io.IOException;
import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.impl.stomp.StompProtocolException;
import org.maps.network.protocol.impl.stomp.listener.ClientFrameListener;

public abstract class ClientFrame extends Frame {

  protected boolean endOfHeader;
  protected boolean hasEndOfFrame;
  private ClientFrameListener frameListener;

  public ClientFrame() {
    endOfHeader = false;
    hasEndOfFrame = false;
  }

  public abstract boolean isValid();

  // This class doesn't throw it, however, extending classes do
  @java.lang.SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
  public void parseCompleted() throws IOException {
    receipt = removeHeader("receipt");
  }

  public int getReceiveMaximum(){
    String val = getHeader("receivemaximum");
    if(val != null){
      try {
        return Integer.parseInt(val.trim());
      } catch (NumberFormatException e) {
        // Invalid number supplied so just return 0 and use the default
      }
    }
    return 0;
  }

  public void scanFrame(Packet packet) throws IOException {
    scanFrame(packet, true);
  }

  public void scanFrame(Packet packet, boolean scanForEnd) throws IOException {
    if (hasEndOfFrame) {
      resume(packet);
    } else {
      int lastValidPos = 0;
      int pos;
      if (!endOfHeader) {
        pos = packet.position();
        lastValidPos = loadHeader(packet, pos);
      }

      if (endOfHeader && packet.hasRemaining() && scanForEnd && packet.get() == END_OF_FRAME) {
        parseCompleted();
        hasEndOfFrame = true;
        return;
      }
      packet.position(lastValidPos);
      throw new EndOfBufferException("Expecting End Of Frame 0x0");
    }
  }

  private int loadHeader(Packet packet, int pos) {
    StringBuilder keyBuilder = new StringBuilder();
    StringBuilder valBuilder = new StringBuilder();
    boolean isKey = true;
    int lastValidPos = pos;
    while (packet.limit() != packet.position() && !endOfHeader) {
      pos++;
      byte t = packet.get();
      if (t == Frame.END_OF_LINE) {
        if (isKey && keyBuilder.length() == 0) {
          endOfHeader = true;
          lastValidPos = pos;
          break;
        } else {
          putHeader(keyBuilder.toString(), valBuilder.toString());
          keyBuilder = new StringBuilder();
          valBuilder = new StringBuilder();
          isKey = true;
          lastValidPos = pos;
        }
      } else if (t == DELIMITER) {
        isKey = false;
      } else {
        if (isKey) {
          keyBuilder.append((char) t);
        } else {
          valBuilder.append((char) t);
        }
      }
    }
    return lastValidPos;
  }


  protected int parseHeaderInt(String key, int def){
    String value = getHeader(key);
    if (value != null) {
      try {
        return Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        // We ignore this as its not a number and return the default
      }
    }
    return def;
  }

  protected long parseHeaderLong(String key, long def){
    String value = getHeader(key);
    if (value != null) {
      try {
        return Long.parseLong(value.trim());
      } catch (NumberFormatException e) {
        // We ignore this as its not a number and return the default
      }
    }
    return def;
  }


  public void resume(Packet packet) throws EndOfBufferException, StompProtocolException {
  }

  public ClientFrameListener getFrameListener() {
    return frameListener;
  }

  public void setListener(ClientFrameListener frameListener) {
    this.frameListener = frameListener;
  }
}
