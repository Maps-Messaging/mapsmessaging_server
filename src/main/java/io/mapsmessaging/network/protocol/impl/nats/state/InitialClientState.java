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

package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import io.mapsmessaging.network.protocol.impl.nats.frames.ConnectFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.listener.FrameListener;

import java.io.IOException;

public class InitialClientState implements State {

  public void handleFrame(SessionState engine, NatsFrame frame, boolean endOfBuffer) throws IOException {
    if (frame instanceof ConnectFrame) {
      FrameListener listener = frame.getListener();
      listener.frameEvent(frame, engine, endOfBuffer);
      listener.postFrameHandling(frame, engine);
    } else {
      throw new NatsProtocolException("Invalid frame received");
    }
  }

  @Override
  public boolean sendMessage(SessionState engine, String destinationName, SubscriptionContext info, Message message, Runnable completionTask) {
    return false;
  }
}
