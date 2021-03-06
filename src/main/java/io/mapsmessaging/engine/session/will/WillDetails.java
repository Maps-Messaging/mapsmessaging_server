/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package io.mapsmessaging.engine.session.will;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.serializer.SerializedObject;
import io.mapsmessaging.utilities.streams.ObjectReader;
import io.mapsmessaging.utilities.streams.ObjectWriter;
import java.io.IOException;

public class WillDetails implements SerializedObject {

  private final long delay;
  private final String sessionId;
  private final String protocol;
  private final String version;

  private String destination;
  private Message msg;

  public WillDetails(Message msg, String destination, long delay, String sessionId, String protocol, String version) {
    this.msg = msg;
    this.destination = destination;
    this.delay = delay;
    this.sessionId = sessionId;
    this.protocol = protocol;
    this.version = version;
  }

  public WillDetails(ObjectReader reader) throws IOException {
    destination = reader.readString();
    delay = reader.readLong();
    sessionId = reader.readString();
    protocol = reader.readString();
    version = reader.readString();
    msg = new Message(reader);
  }

  public void write(ObjectWriter writer) throws IOException {
    writer.write(destination);
    writer.write(delay);
    writer.write(sessionId);
    writer.write(protocol);
    writer.write(version);
    msg.write(writer);
  }

  public void updateTopic(String topic) {
    this.destination = topic;
  }

  public void updateMessage(Message message) {
    msg = message;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getVersion() {
    return version;
  }

  public String getDestination() {
    return destination;
  }

  public Message getMsg() {
    return msg;
  }

  public long getDelay() {
    return delay;
  }

  @Override
  public String toString() {
    return "WillTask:SessionId" + sessionId + " Destination" + destination + " Message:" + msg + " Delay:" + delay;
  }
}
