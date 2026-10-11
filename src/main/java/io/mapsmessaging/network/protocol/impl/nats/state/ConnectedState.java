/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import io.mapsmessaging.network.protocol.impl.nats.listener.FrameListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


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
    PayloadFrame template = engine.buildPayloadFrame(message, destinationName);
    List<PayloadFrame> outgoing = new ArrayList<>();
    List<SubscriptionContext> direct = engine.getSubscriptions().get(destinationName);
    addSubscriptionFrames(outgoing, template, direct);
    for (String destinationKey : engine.getSubscriptions().keySet()) {
      if ((destinationKey.contains("#") || destinationKey.contains("+")) && DestinationSet.matches(destinationKey, destinationName)) {
        addSubscriptionFrames(outgoing, template, engine.getSubscriptions().get(destinationKey));
      }
    }

    if (outgoing.isEmpty()) {
      if (completionTask != null) {
        completionTask.run();
      }
      return true;
    }

    AtomicInteger outstanding = new AtomicInteger(outgoing.size());
    for (PayloadFrame frame : outgoing) {
      frame.setCallback(() -> {
        if (outstanding.decrementAndGet() == 0 && completionTask != null) {
          completionTask.run();
        }
      });
      engine.send(frame);
    }
    return true;
  }

  private void addSubscriptionFrames(List<PayloadFrame> outgoing, PayloadFrame template, List<SubscriptionContext> subscriptions) {
    if (subscriptions == null) {
      return;
    }
    for (SubscriptionContext subscription : subscriptions) {
      PayloadFrame frame = template.duplicate();
      frame.setSubscriptionId(subscription.getAlias());
      outgoing.add(frame);
    }
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
