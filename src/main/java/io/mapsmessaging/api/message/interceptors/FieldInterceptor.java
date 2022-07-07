/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.api.message.interceptors;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import java.util.LinkedHashMap;

public class FieldInterceptor {

  private static final FieldInterceptor instance = new FieldInterceptor();

  public static FieldInterceptor getInstance() {
    return instance;
  }


  private final LinkedHashMap<String, Interceptor> mapLookup;

  private FieldInterceptor() {
    mapLookup = new LinkedHashMap<>();
    mapLookup.put("JMSPriority", new JMSPriorityInterceptor());
    mapLookup.put("JMSTimestamp", new JMSTimestamp());
    mapLookup.put("JMSDeliveryMode", new JMSDeliveryMode());
  }


  public TypedData lookup(Message message, String key) {
    Interceptor interceptor = mapLookup.get(key);
    if (interceptor != null) {
      Object val = interceptor.get(message);
      if (val != null) {
        return new TypedData(val);
      }
    }
    return null;

  }
}
