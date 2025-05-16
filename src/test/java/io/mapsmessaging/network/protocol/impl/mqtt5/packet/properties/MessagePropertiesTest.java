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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MessagePropertiesTest {


  @Test
  void simpleFactoryLookupTest(){
    MessagePropertyFactory factory = MessagePropertyFactory.getInstance();
    for(Integer identifier:factory.getProperties().keySet()){
      MessageProperty property = factory.find(identifier);
      Assertions.assertNotNull(property);
    }
  }

  @Test
  void simpleMessagePropertyInstance(){
    MessagePropertyFactory factory = MessagePropertyFactory.getInstance();
    for(Map.Entry<Integer, MessageProperty> entry:factory.getProperties().entrySet()){
      MessageProperty check = entry.getValue().instance();
      Assertions.assertEquals(entry.getValue().getClass().toString(), check.getClass().toString());
      Assertions.assertEquals(entry.getValue().getName(), check.getName());
    }
  }

}
