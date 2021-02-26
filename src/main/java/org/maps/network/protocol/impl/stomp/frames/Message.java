/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.stomp.frames;

public class Message extends Event {

  private static final byte[] COMMAND = "MESSAGE".getBytes();

  public Message(int maxBufferSize) {
    super(maxBufferSize);
  }

  @Override
  public Frame instance() {
    return new Message(maxBufferSize);
  }

  byte[] getCommand() {
    return COMMAND;
  }

  @Override
  public void packMessage(String destination, String subscriptionId, org.maps.messaging.api.message.Message internalMessage) {
    super.packMessage(destination, subscriptionId, internalMessage);

    // This is only present in MESSAGE
    putHeader("subscription", subscriptionId);
    putHeader("message-id", "" + internalMessage.getIdentifier());
    putHeader("priority", "" + internalMessage.getPriority());
  }

  @Override
  public String toString() {
    return "STOMP Message[ Header:" + getHeaderAsString() + "]";
  }
}
