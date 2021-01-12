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

package org.maps.network.protocol.impl.stomp.state;

import java.io.IOException;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.network.protocol.impl.stomp.StompProtocolException;
import org.maps.network.protocol.impl.stomp.frames.ClientFrame;
import org.maps.network.protocol.impl.stomp.frames.CompletionHandler;
import org.maps.network.protocol.impl.stomp.frames.Error;
import org.maps.network.protocol.impl.stomp.listener.ClientFrameListener;

public class ConnectedState implements State {

  public void handleFrame(StateEngine engine, ClientFrame frame, boolean endOfBuffer) throws IOException {
    ClientFrameListener listener = frame.getFrameListener();
    if (listener != null) {
      try {
        listener.frameEvent(frame, engine, endOfBuffer);
        listener.postFrameHandling(frame, engine);
        frame.complete();
      } catch (StompProtocolException e) {
        Error error = new Error();
        error.setContent(e.getMessage().getBytes());
        if (frame.getReceipt() != null) {
          error.setReceipt(frame.getReceipt());
        }
        engine.send(error);
        frame.setReceipt(null); // its been handled
      }
    } else {
      throw new StompProtocolException("Unhandled Client Frame received");
    }
  }

  @Override
  public boolean sendMessage(StateEngine engine, Destination destination,String normalisedName,  SubscriptionContext context, Message message, Runnable completionTask) {
    org.maps.network.protocol.impl.stomp.frames.Message msg = new org.maps.network.protocol.impl.stomp.frames.Message(message, normalisedName, context.getAlias());
    msg.setCallback(new MessageCompletionHandler(completionTask));
    return engine.send(msg);
  }

  static class MessageCompletionHandler implements CompletionHandler {

    private final Runnable sessionCompletion;

    MessageCompletionHandler(Runnable runnable) {
      sessionCompletion = runnable;
    }

    @Override
    public void run() {
      sessionCompletion.run();
    }
  }
}
