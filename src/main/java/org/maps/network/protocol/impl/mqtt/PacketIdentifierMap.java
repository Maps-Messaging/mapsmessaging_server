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

package org.maps.network.protocol.impl.mqtt;

import org.maps.messaging.api.SubscribedEventManager;

public class PacketIdentifierMap {

  private final int packetIdentifier;
  private final SubscribedEventManager subscription;
  private final long messageId;

  public PacketIdentifierMap(int packetIdentifier, SubscribedEventManager subscription, long messageId) {
    this.packetIdentifier = packetIdentifier;
    this.subscription = subscription;
    this.messageId = messageId;
  }

  public int getPacketIdentifier() {
    return packetIdentifier;
  }

  public SubscribedEventManager getSubscription() {
    return subscription;
  }

  public long getMessageId() {
    return messageId;
  }

  @Override
  public String toString(){
    return "Message ID:"+messageId+" Packet ID:"+packetIdentifier+" Subscriber Id:"+subscription.toString();
  }
}
