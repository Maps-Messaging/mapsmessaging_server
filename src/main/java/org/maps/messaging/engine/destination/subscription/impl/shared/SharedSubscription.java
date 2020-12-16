/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.maps.messaging.engine.destination.subscription.impl.shared;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.subscription.Subscription;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.messaging.engine.destination.subscription.impl.DestinationSubscription;
import org.maps.messaging.engine.destination.subscription.state.MessageStateManager;
import org.maps.messaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.maps.messaging.engine.session.SessionImpl;

public class SharedSubscription extends DestinationSubscription {

  private final SessionSubscriptionMap subscriptions;
  private final String shareName;

  public SharedSubscription(DestinationImpl destinationImpl,
      SubscriptionContext info,
      String id,
      MessageStateManager messageStateManager,
      AcknowledgementController acknowledgementController,
      String shareName) {
    super(destinationImpl, info, null, id, acknowledgementController, messageStateManager);
    subscriptions = new SessionSubscriptionMap();
    this.shareName = shareName;
  }

  @Override
  public void close() {
    super.close();
    subscriptions.close();
    SharedSubscriptionManager register = destinationImpl.findShareRegister(destinationImpl.getName());
    register.delete(shareName);
    if(register.isEmpty()) {
      destinationImpl.delShareRegistry(destinationImpl.getName());
    }
  }

  public Subscription addSession(SessionImpl sessionImpl, String sessionId, SubscriptionContext context, AcknowledgementController acknowledgementController) {
    SessionSharedSubscription subscription = new SessionSharedSubscription(this, sessionImpl, sessionId, context, acknowledgementController);
    subscriptions.add(subscription);

    if (hibernating && sessionImpl != null) {
      wakeUp(sessionImpl);
      destinationImpl.addSubscription(this);
    }
    else{
      schedule();
    }
    return subscription;
  }

  public void removeSession(SessionImpl remove) {
    subscriptions.remove(remove);
    if (subscriptions.isEmpty()) {
      destinationImpl.removeSubscription(getSessionId());
      close();
    }
  }

  @Override
  public void sendMessage(Message message) {
    int loopCount = subscriptions.size();
    while (loopCount > 0) {
      SessionSharedSubscription subscription = subscriptions.pollNext();
      loopCount--;
      if (subscription != null &&
          subscription.getSessionImpl() != null &&
          subscription.canSend()) {
        subscription.sendMessage(message);
        return;
      }
    }
    rollbackReceived(message.getIdentifier());
    // Unable to send for any number of reasons
  }

  @Override
  protected boolean isReady(){
    boolean response = super.isReady();
    if(response){
      for(SessionSharedSubscription subscription:subscriptions.flatMap){
        if(subscription.canSend()){
          return true;
        }
      }
      response = false; // There are NO subscriptions that can actually send events
    }
    return response;
  }

  private static class SessionSubscriptionMap {

    private final Map<String, SessionSharedSubscription> lookupMap;
    private final List<SessionSharedSubscription> flatMap;
    private int idx;

    public SessionSubscriptionMap() {
      lookupMap = new LinkedHashMap<>();
      flatMap = new ArrayList<>();
      idx = -1;
    }

    public void add(SessionSharedSubscription subscription) {
      // If the put returns non null it means its being replaced
      if (lookupMap.put(subscription.getSessionId(), subscription) == null) {
        flatMap.add(subscription);
      }
    }

    public void close() {
      lookupMap.clear();
      flatMap.clear();
      idx = -1;
    }

    public int size() {
      return flatMap.size();
    }

    public void remove(SessionImpl sessionImpl) {
      if (sessionImpl != null) {
        Subscription sub = lookupMap.remove(sessionImpl.getName());
        if (sub != null) {
          flatMap.remove(sub);
        }
        if (!flatMap.isEmpty()) {
          idx = (idx % flatMap.size());
        } else {
          idx = -1;
        }
      }
    }

    public boolean isEmpty() {
      return flatMap.isEmpty();
    }

    public SessionSharedSubscription pollNext() {
      if (!flatMap.isEmpty()) {
        idx = (idx + 1) % flatMap.size();
        return flatMap.get(idx);
      }
      return null;
    }
  }
}
