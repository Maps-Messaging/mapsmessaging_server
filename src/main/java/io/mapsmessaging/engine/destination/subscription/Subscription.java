/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.session.SessionImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public abstract class Subscription implements Runnable, SubscribedEventManager, Subscribable {

  protected final List<SubscriptionContext> contextList;
  protected SessionImpl sessionImpl;
  protected boolean hibernating;

  protected Subscription(SessionImpl sessionImpl, SubscriptionContext context) {
    this.sessionImpl = sessionImpl;
    contextList = new ArrayList<>();
    contextList.add(context);
    hibernating = sessionImpl == null;
  }

  @Override
  public @Nullable SubscriptionContext getContext() {
    if (!contextList.isEmpty()) {
      return contextList.get(0);
    }
    return null;
  }

  public abstract void delete() throws IOException;

  public void addContext(SubscriptionContext context) {
    contextList.add(context);
  }

  @Override
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

  @Override
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
