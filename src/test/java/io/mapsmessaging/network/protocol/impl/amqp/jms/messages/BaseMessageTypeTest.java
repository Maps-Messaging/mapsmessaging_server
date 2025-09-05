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
import org.junit.jupiter.api.Assertions;

public class BaseMessageTypeTest extends MessageTypeTest {

  @Override
  protected Message getMessage(Session session) throws JMSException {
    Message message = session.createMessage();
    message.setIntProperty("IntProperty", 100);
    message.setLongProperty("LongProperty", 1L << 32);
    message.setStringProperty("StringProperty", "This is a generic message property");
    return message;
  }

  @Override
  protected void testMessage(Message message) throws JMSException {
    Assertions.assertNotNull(message);
    Assertions.assertEquals(100, message.getIntProperty("IntProperty"));
    Assertions.assertEquals(1L << 32, message.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a generic message property", message.getStringProperty("StringProperty"));

  }
}
