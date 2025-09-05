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

package io.mapsmessaging.api.message.interceptors;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.message.interceptors.impl.*;

import java.util.LinkedHashMap;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class FieldInterceptor {
  private static class Holder {
    static final FieldInterceptor INSTANCE = new FieldInterceptor();
  }

  public static FieldInterceptor getInstance() {
    return Holder.INSTANCE;
  }

  private final LinkedHashMap<String, Interceptor> mapLookup;

  private FieldInterceptor() {
    mapLookup = new LinkedHashMap<>();
    mapLookup.put("JMSMessageID", new JMSMessageIdInterceptor());
    mapLookup.put("JMSTimestamp", new JMSTimestampInterceptor());
    mapLookup.put("JMSCorrelationID", new JMSCorrelationIdInterceptor());
    mapLookup.put("JMSReplyTo", new JMSReplyToInterceptor());
    mapLookup.put("JMSDeliveryMode", new JMSDeliveryModeInterceptor());
    mapLookup.put("JMSType", new JMSTypeInterceptor());
    mapLookup.put("JMSExpiration", new JMSExpirationInterceptor());
    mapLookup.put("JMSPriority", new JMSPriorityInterceptor());
    mapLookup.put("MapsMsgPayload", new OpaqueDataInterceptor());
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
