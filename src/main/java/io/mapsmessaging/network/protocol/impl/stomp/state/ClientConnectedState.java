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

package io.mapsmessaging.network.protocol.impl.stomp.state;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Send;

public class ClientConnectedState extends ConnectedState {

  @Override
  public boolean sendMessage(SessionState engine, String destinationName, SubscriptionContext context, Message message, Runnable completionTask) {
    Send msg = new Send(1024, engine.getProtocol().isBase64Encode());
    msg.packMessage(destinationName, message);
    msg.setCallback(new MessageCompletionHandler(completionTask));
    return engine.send(msg);
  }
}
