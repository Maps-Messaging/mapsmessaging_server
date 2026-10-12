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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    List<SubscriptionContext> matched = resolveSubscriptionContexts(engine, destinationName, context);
    List<SubscriptionContext> recipients = selectRecipients(engine, destinationName, matched);
    if (recipients.isEmpty()) {
      if (completionTask != null) {
        completionTask.run();
      }
      return true;
    }

    PayloadFrame template = engine.buildPayloadFrame(message, destinationName);
    AtomicInteger outstanding = new AtomicInteger(recipients.size());
    for (SubscriptionContext subscription : recipients) {
      PayloadFrame frame = template.duplicate();
      frame.setSubscriptionId(subscription.getAlias());
      frame.setCallback(() -> {
        if (outstanding.decrementAndGet() == 0 && completionTask != null) {
          completionTask.run();
        }
      });
      if (!engine.send(frame)) {
        frame.complete();
      }
    }
    return true;
  }

  private List<SubscriptionContext> resolveSubscriptionContexts(SessionState engine, String destinationName, SubscriptionContext deliveredContext) {
    synchronized (engine) {
      Map<String, List<SubscriptionContext>> subscriptions = engine.getSubscriptions();
      String deliveredAlias = deliveredContext == null ? null : deliveredContext.getAlias();
      if (deliveredAlias != null) {
        for (List<SubscriptionContext> contexts : subscriptions.values()) {
          if (contexts.stream().anyMatch(item -> deliveredAlias.equals(item.getAlias()))) {
            return new ArrayList<>(contexts);
          }
        }
      }

      List<SubscriptionContext> direct = subscriptions.get(destinationName);
      if (direct != null) {
        return new ArrayList<>(direct);
      }
      List<SubscriptionContext> wildcardMatches = new ArrayList<>();
      for (Map.Entry<String, List<SubscriptionContext>> entry : subscriptions.entrySet()) {
        String filter = entry.getKey();
        if ((filter.contains("#") || filter.contains("+")) && DestinationSet.matches(filter, destinationName)) {
          wildcardMatches.addAll(entry.getValue());
        }
      }
      return wildcardMatches;
    }
  }

  private List<SubscriptionContext> selectRecipients(SessionState engine, String destinationName, List<SubscriptionContext> matches) {
    List<SubscriptionContext> recipients = new ArrayList<>();
    Map<String, List<SubscriptionContext>> queueGroups = new LinkedHashMap<>();
    Set<String> aliases = new HashSet<>();
    for (SubscriptionContext subscription : matches) {
      if (!aliases.add(subscription.getAlias())) {
        continue;
      }
      String sharedName = subscription.getSharedName();
      if (sharedName == null || sharedName.isEmpty()) {
        recipients.add(subscription);
      } else {
        queueGroups.computeIfAbsent(sharedName, ignored -> new ArrayList<>()).add(subscription);
      }
    }
    for (Map.Entry<String, List<SubscriptionContext>> group : queueGroups.entrySet()) {
      List<SubscriptionContext> members = group.getValue();
      int index = engine.nextQueueGroupIndex(destinationName, group.getKey(), members.size());
      recipients.add(members.get(index));
    }
    return recipients;
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
