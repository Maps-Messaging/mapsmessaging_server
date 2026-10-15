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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.tasks.Response;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public abstract class Subscription implements Runnable, SubscribedEventManager, Subscribable {

  protected final List<SubscriptionContext> contextList;
  @Getter
  protected SessionImpl sessionImpl;
  @Getter
  protected boolean hibernating;

  protected Subscription(SessionImpl sessionImpl, SubscriptionContext context) {
    this.sessionImpl = sessionImpl;
    contextList = new ArrayList<>();
    contextList.add(context);
    hibernating = sessionImpl == null;
  }

  @Override
  public @Nullable SubscriptionContext getContext() {
    synchronized (contextList) {
      return contextList.isEmpty() ? null : contextList.get(0);
    }
  }

  /** Existing session journal used by restored offline subscriptions before first reconnect. */
  public void setDeliveryStateCoordinator(io.mapsmessaging.engine.session.DeliveryStateCoordinator coordinator) {
    // Subscription implementations without protocol delivery state require no attachment.
  }

  public void setDeliveryJournalPath(java.nio.file.Path path) {
    // Subscription implementations without protocol delivery state require no attachment.
  }

  public java.util.concurrent.CompletableFuture<Void> acknowledgeRecoveredDelivery(long messageId) {
    return java.util.concurrent.CompletableFuture.failedFuture(
        new UnsupportedOperationException("Subscription cannot acknowledge recovered delivery"));
  }

  protected java.util.concurrent.CompletableFuture<Void> submitRecoveredAcknowledgement(
      io.mapsmessaging.engine.destination.DestinationImpl destination, Runnable acknowledgement) {
    java.util.concurrent.CompletableFuture<Void> completion = new java.util.concurrent.CompletableFuture<>();
    try {
      destination.submit(new io.mapsmessaging.engine.tasks.EngineTask() {
        @Override
        public Response taskCall() {
          try {
            acknowledgement.run();
            completion.complete(null);
            return new io.mapsmessaging.engine.tasks.VoidResponse();
          } catch (RuntimeException error) {
            completion.completeExceptionally(error);
            throw error;
          }
        }
      });
    } catch (RuntimeException error) {
      completion.completeExceptionally(error);
    }
    return completion;
  }

  public abstract void delete() throws IOException;

  public void addContext(SubscriptionContext context) {
    synchronized (contextList) {
      contextList.add(context);
    }
  }

  @Override
  public List<SubscriptionContext> getContexts() {
    synchronized (contextList) {
      return new ArrayList<>(contextList);
    }
  }

  public boolean isContextEmpty(){
    synchronized (contextList) {
      return contextList.isEmpty();
    }
  }

  public void removeContext(SubscriptionContext context) {
    synchronized (contextList) {
      contextList.remove(context);
    }
  }

  public void hibernate() {
    sessionImpl = null;
    hibernating = true;
  }

  public void wakeUp(SessionImpl sessionImpl) {
    this.sessionImpl = sessionImpl;
    hibernating = false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Context:");
    for (SubscriptionContext context : getContexts()) {
      sb.append(context.toString()).append(",");
    }
    return sb.toString();
  }

  public abstract void cancel() throws IOException;

  public abstract String getSessionId();

  public abstract void sendMessage(Message message);

  public abstract Future<Response> getNext() throws IOException;

  public abstract String getAcknowledgementType();

  public abstract SubscriptionStateDTO getState();

}
