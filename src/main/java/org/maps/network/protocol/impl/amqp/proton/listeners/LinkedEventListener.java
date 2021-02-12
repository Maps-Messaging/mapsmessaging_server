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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.EventType;

public class LinkedEventListener implements EventListener {

  private final List<EventListener> listeners;
  private final EventType type;

  public LinkedEventListener(EventListener... list){
    this.listeners = new ArrayList<>();
    Collections.addAll(listeners, list);
    this.type = listeners.get(0).getType();
  }

  @Override
  public boolean handleEvent(Event event) {
    boolean response = false;
    for(EventListener listener:listeners){
      if(listener.handleEvent(event)){
        response = true;
      }
    }
    return response;
  }

  @Override
  public EventType getType() {
    return type;
  }
}
