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

package io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.impl.encoders;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.api.message.TypedData.TYPE;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApplicationMapEncoder {

  public static void unpackApplicationMap(Map<String, TypedData> dataMap, org.apache.qpid.proton.message.Message protonMessage) {
    ApplicationProperties applicationProperties = protonMessage.getApplicationProperties();
    if (applicationProperties != null) {
      Map<String, Object> map = applicationProperties.getValue();
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
      }
    }
  }

  public static void packApplicationMap(Message message, org.apache.qpid.proton.message.Message protonMessage) {
    Map<String, Object> applicationMap = new LinkedHashMap<>();
    Map<String, TypedData> dataMap = message.getDataMap();
    for (Map.Entry<String, TypedData> entry : dataMap.entrySet()) {
      boolean add = true;
      for (String testKey : PropertiesEncoder.propertyNames) {
        if (testKey.equals(entry.getKey())) {
          add = false;
          break;
        }
      }
      if (add) {
        if (entry.getValue().getType() == TYPE.BYTE_ARRAY) {
          applicationMap.put(entry.getKey(), new Binary((byte[]) entry.getValue().getData()));
        } else {
          applicationMap.put(entry.getKey(), entry.getValue().getData());
        }
      }
    }
    protonMessage.setApplicationProperties(new ApplicationProperties(applicationMap));
  }

  private ApplicationMapEncoder() {
  }
}
