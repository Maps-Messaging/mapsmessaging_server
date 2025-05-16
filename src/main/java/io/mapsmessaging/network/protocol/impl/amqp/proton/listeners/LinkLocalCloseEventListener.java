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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Event.Type;
import org.apache.qpid.proton.engine.EventType;
import org.apache.qpid.proton.engine.Link;

public class LinkLocalCloseEventListener extends BaseEventListener {

  public LinkLocalCloseEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Session session = (Session) event.getSession().getContext();
    if (session != null) {
      Link link = event.getLink();
      SubscribedEventManager eventManager = (SubscribedEventManager) link.getContext();
      if (eventManager != null) {
        if (!eventManager.getContexts().isEmpty()) {
          String alias = eventManager.getContext().getAlias();
          session.removeSubscription(alias);
          engine.getSubscriptions().remove(alias);
          protocol.getLogger().log(ServerLogMessages.AMQP_DELETED_SUBSCRIPTION, alias);
        }
        link.setContext(null);
        return true;
      }
    }
    return false;
  }

  @Override
  public EventType getType() {
    return Type.LINK_LOCAL_CLOSE;
  }
}
