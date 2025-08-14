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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl;

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.Message;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class BaseModemProtocol {

  protected final Modem modem;

  protected BaseModemProtocol(Modem modem) {
    this.modem = modem;
  }

  /*
  Handle outgoing messages
   */
  public abstract void sendMessage(Message message);

  public abstract CompletableFuture<List<SendMessageState>> listSentMessages();

  public abstract CompletableFuture<Void> deleteSentMessages(String msgName);

  public abstract CompletableFuture<Void> markSentMessageRead(String name);

  public abstract void listOutgoingMessages();

  /*
  Handle incoming messages
   */
  public abstract CompletableFuture<List<String>> listIncomingMessages();

  public abstract CompletableFuture<byte[]> getMessage(String metaLine, MessageFormat format);

  public abstract CompletableFuture<Void> markMessageRetrieved(String metaLine);

  public abstract CompletableFuture<List<byte[]>> fetchAllMessages(MessageFormat format);

  public abstract String getType();
}
