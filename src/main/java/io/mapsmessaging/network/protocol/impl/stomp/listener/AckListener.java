/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Ack;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Error;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;

public class AckListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, SessionState engine, boolean endOfBuffer) {
    Ack ackFrame = (Ack) frame;
    SubscribedEventManager subscription = engine.findSubscription(ackFrame.getSubscription());
    if (subscription != null) {
      try {
        long messageId = Long.parseLong(ackFrame.getMessageId().trim());
        subscription.ackReceived(messageId);
      } catch (NumberFormatException e) {
        if (ackFrame.getReceipt() != null) {
          Error error = new Error();
          error.setReceipt(ackFrame.getReceipt());
          error.setContent((e.getMessage()).getBytes());
          error.setContentType("text/plain");
          ackFrame.setReceipt(null);
          engine.send(error);
        }
      }
    } else {
      Error error = new Error();
      error.setReceipt(ackFrame.getReceipt());
      error.setContent(("No subscription found that matches : " + ackFrame.getSubscription()).getBytes());
      error.setContentType("text/plain");
      ackFrame.setReceipt(null);
      engine.send(error);
    }
  }
}
