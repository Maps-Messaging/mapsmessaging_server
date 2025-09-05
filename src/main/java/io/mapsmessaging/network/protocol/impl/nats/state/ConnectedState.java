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
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import io.mapsmessaging.network.protocol.impl.nats.frames.CompletionHandler;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.listener.FrameListener;

import java.io.IOException;
import java.util.List;


public class ConnectedState implements State {

  public void handleFrame(SessionState engine, NatsFrame frame, boolean endOfBuffer) throws IOException {
    FrameListener listener = frame.getListener();
    if (listener != null) {
      try {
        listener.frameEvent(frame, engine, endOfBuffer);
        listener.postFrameHandling(frame, engine);
        frame.complete();
      } catch (NatsProtocolException e) {
        ErrFrame error = new ErrFrame();
        error.setError(e.getMessage());
        if (frame.getReceipt() != null) {
          error.setReceipt(frame.getReceipt());
        }
        engine.send(error);
        frame.setReceipt(null); // its been handled
      }
    } else {
      throw new NatsProtocolException("Unhandled Client Frame received");
    }
  }

  @Override
  public boolean sendMessage(SessionState engine, String destinationName, SubscriptionContext context, Message message, Runnable completionTask) {
    PayloadFrame msg = engine.buildPayloadFrame(message, destinationName);
    // This handles non-wildcard subscriptions
    List<SubscriptionContext> subscriptions = engine.getSubscriptions().get(destinationName);
    PayloadFrame duplicate = msg;
    if (subscriptions != null) {
      duplicate = sendToList(duplicate, subscriptions, engine);
    }

    // Now handle wildcard subscriptions
    for (String destinationKey : engine.getSubscriptions().keySet()) {
      if ((destinationKey.contains("#") || destinationKey.contains("+")) && DestinationSet.matches(destinationKey, destinationName)) {
        List<SubscriptionContext> subContext = engine.getSubscriptions().get(destinationKey);
        duplicate = sendToList(duplicate, subContext, engine);
      }
    }

    NamedConsumer named =  engine.getNamedConsumers().get(context.getAlias());
    if(named != null) {
      named.receive(message, destinationName, completionTask);
    }
    completionTask.run();
    return true;
  }


  private PayloadFrame sendToList(PayloadFrame duplicate, List<SubscriptionContext> subscriptions, SessionState engine) {
    boolean multiple = subscriptions.size() > 1;
    for (SubscriptionContext duplicates : subscriptions) {
      duplicate.setSubscriptionId(duplicates.getAlias());
      engine.send(duplicate);
      if (multiple) duplicate = duplicate.duplicate();
      duplicate.setCallback(null);
    }
    return duplicate;
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
