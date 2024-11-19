/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.engine.session.persistence;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.utilities.PersistentObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class SessionDetails extends PersistentObject {

  @Getter
  @Setter
  private String sessionName;

  @Getter
  @Setter
  private String uniqueId;

  @Getter
  @Setter
  private List<SubscriptionContext> subscriptionContextList = new ArrayList<>();

  public SessionDetails() {
  }


  public SessionDetails(InputStream inputStream) throws IOException {
    sessionName = readString(inputStream);
    uniqueId = readString(inputStream);
    int subListSize = readInt(inputStream);
    for(int x=0;x<subListSize;x++){
      subscriptionContextList.add(new SubscriptionContext(inputStream));
    }
  }

  public SessionDetails(String sessionName, String uniqueId) {
    this.sessionName = sessionName;
    this.uniqueId = uniqueId;
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
    writeString(outputStream, sessionName);
    writeString(outputStream, uniqueId);
    writeInt(outputStream, subscriptionContextList.size());
    for(SubscriptionContext subscriptionContext:subscriptionContextList){
      subscriptionContext.save(outputStream);
    }
  }

}
