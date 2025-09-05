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
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Assertions;

public class TextMessageTypeTest extends MessageTypeTest {
  @Override
  protected Message getMessage(Session session) throws JMSException {
    TextMessage textMessage = session.createTextMessage("Hello world! this should be a text message");
    textMessage.setIntProperty("IntProperty", 1);
    textMessage.setLongProperty("LongProperty", 1 << 2);
    textMessage.setStringProperty("StringProperty", "This is a string property");
    return textMessage;
  }

  @Override
  protected void testMessage(Message message) throws JMSException {
    Assertions.assertNotNull(message);
    Assertions.assertTrue(message instanceof TextMessage);
    TextMessage message1 = (TextMessage) message;
    Assertions.assertEquals(1, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1 << 2, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
  }
}
