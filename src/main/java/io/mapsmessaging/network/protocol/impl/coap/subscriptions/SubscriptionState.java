/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.coap.subscriptions;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;

import java.util.LinkedHashMap;
import java.util.Map;


public class SubscriptionState {


  private final Map<String, Context> state;

  public SubscriptionState() {
    state = new LinkedHashMap<>();
  }

  public Context create(String path, BasePacket request) {
    Context context = new Context(path, request);
    state.put(path, context);
    return context;
  }

  public Context find(String path){
    return state.get(path);
  }


  public Context remove(String path) {
    return state.remove(path);
  }

  public boolean exists(String path) {
    return state.containsKey(path);
  }

  public boolean isEmpty() {
    return state.isEmpty();
  }
}
