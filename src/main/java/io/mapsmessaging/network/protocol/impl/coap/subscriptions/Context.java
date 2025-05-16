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

package io.mapsmessaging.network.protocol.impl.coap.subscriptions;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Observe;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Option;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.OBSERVE;

public class Context {

  @Getter
  private final String path;

  @Getter
  private final BasePacket request;

  @Getter
  @Setter
  private SubscribedEventManager subscribedEventManager;

  @Getter
  private final boolean observe;

  @Getter
  private final AtomicLong observeId;


  public Context(String path, BasePacket request){
    observeId = new AtomicLong(2);
    this.path = path;
    this.request = request;
    OptionSet optionSet = request.getOptions();
    if(optionSet.hasOption(OBSERVE)){
      Option observeOption = optionSet.getOption(OBSERVE);
      observe = ((Observe)observeOption).register();
    }
    else {
      observe = false;
    }
  }

}
