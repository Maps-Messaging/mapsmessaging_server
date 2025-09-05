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

package io.mapsmessaging.network.protocol.impl.amqp.jms.messages;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import org.junit.jupiter.api.Assertions;

public class ObjectMessageTypeTest extends MessageTypeTest {

  @Override
  protected Message getMessage(Session session) throws JMSException {
    ObjectMessage objectMessage = session.createObjectMessage();
    objectMessage.setIntProperty("IntProperty", 5);
    objectMessage.setLongProperty("LongProperty", 1L << 32);
    objectMessage.setStringProperty("StringProperty", "This is a string property");
    objectMessage.setObject("This Should be a object message of a string");
    return objectMessage;
  }

  @Override
  protected void testMessage(Message message) throws JMSException {
    Assertions.assertNotNull(message);
    Assertions.assertTrue(message instanceof ObjectMessage);
    ObjectMessage message1 = (ObjectMessage) message;
    Assertions.assertEquals(5, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1L << 32, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
    Object result = message1.getObject();
    Assertions.assertTrue(result instanceof String);
    Assertions.assertEquals("This Should be a object message of a string", result);


  }
}
