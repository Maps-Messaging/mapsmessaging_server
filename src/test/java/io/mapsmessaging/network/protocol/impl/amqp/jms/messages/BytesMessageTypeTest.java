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

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.junit.jupiter.api.Assertions;

public class BytesMessageTypeTest extends MessageTypeTest {

  @Override
  protected Message getMessage(Session session) throws JMSException {
    BytesMessage bytesMessage = session.createBytesMessage();
    bytesMessage.setIntProperty("IntProperty", 2);
    bytesMessage.setLongProperty("LongProperty", 1 << 8);
    bytesMessage.setStringProperty("StringProperty", "This is a string property");
    bytesMessage.writeBytes(payload.getBytes());
    return bytesMessage;
  }

  @Override
  protected void testMessage(Message message) throws JMSException {
    Assertions.assertNotNull(message);
    Assertions.assertTrue(message instanceof BytesMessage);
    BytesMessage message1 = (BytesMessage) message;
    Assertions.assertEquals(2, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1 << 8, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
    byte[] test = new byte[payload.length()];
    message1.readBytes(test);
    Assertions.assertEquals(payload, new String(test));
  }
}
