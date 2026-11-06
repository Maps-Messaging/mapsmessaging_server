/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class DuplicationManager {

  private final Map<Integer, CachedResponse> requestResponseMap;

  public DuplicationManager(){
    requestResponseMap = new LinkedHashMap<>();
  }

  public synchronized void put(BasePacket response){
    purgeExpired();
    requestResponseMap.put(
        response.getMessageId(),
        new CachedResponse(response, System.currentTimeMillis() + Constants.EXCHANGE_LIFETIME_MILLIS));
  }

  public synchronized BasePacket getResponse(int messageId){
    purgeExpired();
    CachedResponse cached = requestResponseMap.get(messageId);
    return cached == null ? null : cached.response();
  }

  private void purgeExpired() {
    long now = System.currentTimeMillis();
    Iterator<CachedResponse> iterator = requestResponseMap.values().iterator();
    while (iterator.hasNext()) {
      if (iterator.next().expiresAt() <= now) {
        iterator.remove();
      }
    }
  }

  private record CachedResponse(BasePacket response, long expiresAt) {
  }
}
