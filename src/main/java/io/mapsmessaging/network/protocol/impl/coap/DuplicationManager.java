/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import java.util.ArrayList;
import java.util.List;

public class DuplicationManager {

  private final int nstart; // Depth of outstanding
  private final List<BasePacket> requestResponseMap;

  public DuplicationManager(int nstart){
    requestResponseMap = new ArrayList<>();
    this.nstart = nstart;
  }

  public synchronized void put(BasePacket response){
    requestResponseMap.add(response);
    while(requestResponseMap.size() > nstart){
      requestResponseMap.remove(0).getMessageId();
    }
  }

  public synchronized BasePacket getResponse(int messageId){
    return requestResponseMap.stream().filter(response -> response.getMessageId() == messageId).findFirst().orElse(null);
  }

}
