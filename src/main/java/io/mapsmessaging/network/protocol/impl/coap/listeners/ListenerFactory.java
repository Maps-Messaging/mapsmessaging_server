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

package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory;

public class ListenerFactory {

  private final Listener[] listeners;


  public Listener getListener(int id){
    if(id >=0 && id< listeners.length) {
      return listeners[id];
    }
    return null;
  }


  public ListenerFactory(){
    listeners = new Listener[8];
    for(int x=0;x<listeners.length;x++){
      listeners[x] = new EmptyListener();
    }
    listeners[PacketFactory.DELETE] = new DeleteListener();
    listeners[PacketFactory.EMPTY] = new EmptyListener();
    listeners[PacketFactory.FETCH] = new FetchListener();
    listeners[PacketFactory.GET] = new GetListener();
    listeners[PacketFactory.IPATCH] = new IPatchListener();
    listeners[PacketFactory.PATCH] = new PatchListener();
    listeners[PacketFactory.POST] = new PostListener();
    listeners[PacketFactory.PUT] = new PutListener();
  }
}
