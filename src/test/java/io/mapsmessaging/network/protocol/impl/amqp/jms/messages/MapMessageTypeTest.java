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

package io.mapsmessaging.network.protocol.impl.amqp.jms.messages;

import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.junit.jupiter.api.Assertions;

public class MapMessageTypeTest extends MessageTypeTest {

  @Override
  protected Message getMessage(Session session) throws JMSException {
    MapMessage mapMessage = session.createMapMessage();
    mapMessage.setIntProperty("IntProperty", 3);
    mapMessage.setLongProperty("LongProperty", 1 << 10);
    mapMessage.setStringProperty("StringProperty", "This is a string property");
    mapMessage.setBytes("bytesProperty", new byte[100]);
    mapMessage.setString("body", payload);
    return mapMessage;
  }

  @Override
  protected void testMessage(Message message) throws JMSException {
    Assertions.assertNotNull(message);
    Assertions.assertTrue(message instanceof MapMessage);
    MapMessage message1 = (MapMessage) message;
    Assertions.assertEquals(3, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1 << 10, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
    Assertions.assertEquals(payload, message1.getString("body"));

  }
}