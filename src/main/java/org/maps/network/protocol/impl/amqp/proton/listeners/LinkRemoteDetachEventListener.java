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

package org.maps.network.protocol.impl.amqp.proton.listeners;

import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Event.Type;
import org.apache.qpid.proton.engine.EventType;
import org.apache.qpid.proton.engine.Link;
import org.maps.logging.LogMessages;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.network.protocol.impl.amqp.AMQPProtocol;
import org.maps.network.protocol.impl.amqp.proton.ProtonEngine;

public class LinkRemoteDetachEventListener extends BaseEventListener {


  public LinkRemoteDetachEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    org.maps.messaging.api.Session session = (org.maps.messaging.api.Session) event.getSession().getContext();
    if (session != null) {
      Link link = event.getLink();
      SubscribedEventManager eventManager = (SubscribedEventManager) link.getContext();
      if (eventManager != null) {
        event.getSession().setContext(null);
        SubscriptionContext context = eventManager.getContext();
        if(context != null) {
          String alias = context.getAlias();
          session.hibernateSubscription(alias);
        }
        link.setContext(null);
        link.detach();
        ErrorCondition errorCondition = link.getRemoteCondition();
        if(errorCondition != null){
          protocol.getLogger().log(LogMessages.AMQP_REMOTE_LINK_ERROR, errorCondition);
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public EventType getType() {
    return Type.LINK_REMOTE_DETACH;
  }
}
