/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.amqp.proton.listeners;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.EventType;
import org.maps.network.protocol.impl.amqp.AMQPProtocol;
import org.maps.network.protocol.impl.amqp.proton.ProtonEngine;

public class EventListenerFactory {

  private final Map<EventType, EventListener> listeners;


  public EventListenerFactory(AMQPProtocol protocol, ProtonEngine engine){
    listeners = new LinkedHashMap<>();
    register(new DeliveryEventListener(protocol, engine));
    register(new LinkFlowEventListener(protocol, engine));

    register(new ConnectionLocalOpenEventListener(protocol, engine));

    LinkedEventListener linkLocalOpenList = new LinkedEventListener(
        new SenderLinkLocalOpenEventListener(protocol, engine),
        new ReceiverLinkLocalOpenEventListener(protocol, engine),
        new LinkLocalOpenEventListener(protocol, engine)
    );
    register(linkLocalOpenList);

    register(new LinkLocalCloseEventListener(protocol,engine));

    register(new LinkRemoteOpenEventListener(protocol, engine));
    register(new LinkRemoteCloseEventListener(protocol, engine));
    register(new LinkRemoteDetachEventListener(protocol, engine));

    register(new LinkFinalEventListener(protocol, engine));

    register(new ConnectionRemoteOpenEventListener(protocol, engine));
    register(new ConnectionRemoteCloseEventListener(protocol, engine));

    register(new SessionRemoteOpenEventListener(protocol, engine));
    register(new SessionRemoteCloseEventListener(protocol, engine));
  }

  public void handleEvent(Event event){
    EventListener listener = listeners.get(event.getEventType());
    if (listener != null) {
      listener.handleEvent(event);
    }
  }

  private void register(EventListener listener){
    listeners.put(listener.getType(), listener);
  }

}
