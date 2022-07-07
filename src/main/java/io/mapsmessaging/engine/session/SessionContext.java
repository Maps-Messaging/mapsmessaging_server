/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.storage.impl.streams.ObjectReader;
import io.mapsmessaging.storage.impl.streams.ObjectWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SessionContext {

  // <editor-fold desc="These fields are persisted and on reload describes the session">
  private final String id;
  private String willTopic;
  private Message willMessage;
  private long willDelay;
  private long expiry;
  // </editor-fold>

  // <editor-fold desc="These are volatile fields and must not be persisted since they change at run
  // time">
  private ProtocolImpl protocol;

  private String authenticationMethod;
  private byte[] authenticationData;
  private String username;
  private char[] password;

  private int receiveMaximum;
  private int duration;

  private boolean isRestored;
  private boolean resetState;
  private boolean persistentSession;
  // </editor-fold>

  public SessionContext(String id, ProtocolImpl protocol) {
    this.id = id;
    this.protocol = protocol;
    expiry = -1;
    receiveMaximum = (1 << 16) - 1;
    isRestored = false;
    duration = -1;
  }

  public SessionContext(ObjectReader reader) throws IOException {
    id = reader.readString();
    expiry = reader.readLong();
    if (reader.readByte() != 0) {
      willDelay = reader.readLong();
      willTopic = reader.readString();
      int bufferCount = reader.readInt();
      ByteBuffer[] bb = new ByteBuffer[bufferCount];
      for (int x = 0; x < bb.length; x++) {
        bb[x] = ByteBuffer.wrap(reader.readByteArray());
      }
      willMessage = MessageFactory.getInstance().unpack(bb);
    }
    isRestored = false;
  }

  public void write(ObjectWriter writer) throws IOException {
    writer.write(id);
    writer.write(expiry);
    if (willMessage != null) {
      writer.write((byte) 1);
      writer.write(willDelay);
      writer.write(willTopic);
      ByteBuffer[] buffers = MessageFactory.getInstance().pack(willMessage);
      writer.write(buffers.length);
      for (ByteBuffer buffer : buffers) {
        writer.write(buffer.array());
      }
    } else {
      writer.write((byte) 0);
    }
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public char[] getPassword() {
    return password;
  }

  public void setPassword(char[] password) {
    this.password = password;
  }

  public ProtocolImpl getProtocol() {
    return protocol;
  }

  public boolean isResetState() {
    return resetState;
  }

  public void setResetState(boolean resetState) {
    this.resetState = resetState;
  }

  public String getId() {
    return id;
  }

  public String getWillTopic() {
    return willTopic;
  }

  public void setWillTopic(String willTopic) {
    this.willTopic = willTopic;
  }

  public Message getWillMessage() {
    return willMessage;
  }

  public void setWillMessage(Message willMessage) {
    this.willMessage = willMessage;
  }

  public boolean isPersistentSession() {
    return persistentSession;
  }

  public void setPersistentSession(boolean persistentSession) {
    this.persistentSession = persistentSession;
  }

  public long getWillDelay() {
    return willDelay;
  }

  public void setWillDelay(long willDelay) {
    this.willDelay = willDelay;
  }

  public boolean isRestored() {
    return isRestored;
  }

  public void setRestored(boolean restored) {
    isRestored = restored;
  }

  public long getSessionExpiry() {
    return expiry;
  }

  public void setSessionExpiry(long expiry) {
    this.expiry = expiry;
  }

  public int getReceiveMaximum() {
    return receiveMaximum;
  }

  public void setReceiveMaximum(int receiveMaximum) {
    this.receiveMaximum = receiveMaximum;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  @Override
  public String toString() {
    return "SessionContext:" + "Id:" + id
        + " Protocol:" + protocol.toString()
        + " ResetState:" + resetState
        + " PersistentSession:" + persistentSession
        + " WillDelay:" + willDelay
        + " Expiry:" + expiry
        + " ReceiveMaximum:" + receiveMaximum
        + " Restored:" + isRestored
        + " KeepAlive:" + duration;
  }

  public void setAuthenticationMethod(String authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
  }

  public void setAuthenticationData(byte[] authenticationData) {
    this.authenticationData = authenticationData;
  }

  public String getAuthenticationMethod() {
    return authenticationMethod;
  }

  public byte[] getAuthenticationData() {
    return authenticationData;
  }
}
