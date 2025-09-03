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

package io.mapsmessaging.engine.session.persistence;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.utilities.PersistentObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class SessionDetails extends PersistentObject {

  private int version;
  private String sessionName;
  private String uniqueId;
  private long internalUnqueId;
  private List<SubscriptionContext> subscriptionContextList = new ArrayList<>();

  public SessionDetails() {

  }

  public SessionDetails(String sessionName, String uniqueId, long internalUnqueId) {
    this.sessionName = sessionName;
    this.uniqueId = uniqueId;
    this.internalUnqueId = internalUnqueId;
    version = 2;
  }


  public SessionDetails(InputStream inputStream) throws IOException {
    version = readInt(inputStream);
    boolean hasInternalUnqueId = true;
    if(version != 2) {
      sessionName = new String(readFullBuffer(inputStream, version));
      hasInternalUnqueId = false;
      version = 2;
    }
    else {
      sessionName = readString(inputStream);
    }
    uniqueId = readString(inputStream);
    if(hasInternalUnqueId){
      internalUnqueId = readLong(inputStream);
    }
    else{
      internalUnqueId = 0;
    }
    int subListSize = readInt(inputStream);
    for(int x=0;x<subListSize;x++){
      subscriptionContextList.add(new SubscriptionContext(inputStream, internalUnqueId));
    }
  }

  public Map<String, SubscriptionContext> getSubscriptionContextMap(){
    Map<String, SubscriptionContext> map = new LinkedHashMap<>();
    for(SubscriptionContext context:subscriptionContextList){
      map.put(context.getAlias(), context); // Pre-populate with persistent data
    }
    return map;
  }

  public void clearSubscriptions() {
    subscriptionContextList.clear();
  }

  public void save(OutputStream outputStream) throws IOException {
    writeInt(outputStream, version);
    writeString(outputStream, sessionName);
    writeString(outputStream, uniqueId);
    writeLong(outputStream, internalUnqueId);
    writeInt(outputStream, subscriptionContextList.size());
    for(SubscriptionContext subscriptionContext:subscriptionContextList){
      subscriptionContext.save(outputStream);
    }
  }

}
