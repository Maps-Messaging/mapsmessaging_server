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

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.engine.Event.Type;

public class LinkFlowEventListener extends BaseEventListener {

  public LinkFlowEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Link link = event.getLink();
    if (link instanceof Receiver) {
      topUp((Receiver) link);
    } else if (link instanceof Sender) {
      Sender snd = (Sender) link;
      if (snd.getSource() instanceof Source) {
        SubscribedEventManager eventManager = (SubscribedEventManager) snd.getContext();
        if (eventManager != null) {
          eventManager.updateCredit(snd.getCredit());
        }
        if (snd.getDrain()) {
          snd.drained();
        }
      }
    }
    return true;
  }

  @Override
  public EventType getType() {
    return Type.LINK_FLOW;
  }
}
