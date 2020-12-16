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

package org.maps.messaging.engine.destination.subscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.session.SessionImpl;

public abstract class Subscription implements Runnable, SubscribedEventManager, Subscribable {

  protected final List<SubscriptionContext> contextList;
  protected SessionImpl sessionImpl;
  protected boolean hibernating;

  public Subscription(SessionImpl sessionImpl, SubscriptionContext context) {
    this.sessionImpl = sessionImpl;
    contextList = new ArrayList<>();
    contextList.add(context);
    hibernating = sessionImpl == null;
  }

  public @Nullable SubscriptionContext getContext() {
    if(!contextList.isEmpty()) {
      return contextList.get(0);
    }
    return null;
  }

  public void addContext(SubscriptionContext context) {
    contextList.add(context);
  }

  public List<SubscriptionContext> getContexts() {
    return new ArrayList<>(contextList);
  }

  public void removeContext(SubscriptionContext context) {
    contextList.remove(context);
  }

  public SessionImpl getSessionImpl() {
    return sessionImpl;
  }

  public void hibernate() {
    sessionImpl = null;
    hibernating = true;
  }

  public boolean isHibernating() {
    return hibernating;
  }

  public void wakeUp(SessionImpl sessionImpl) {
    this.sessionImpl = sessionImpl;
    hibernating = false;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("Context:");
    for (SubscriptionContext context : contextList) {
      sb.append(context.toString()).append(",");
    }
    return sb.toString();
  }

  public abstract void cancel() throws IOException;

  public abstract String getSessionId();

  public abstract void sendMessage(Message message);

  public abstract String getAcknowledgementType();

}
