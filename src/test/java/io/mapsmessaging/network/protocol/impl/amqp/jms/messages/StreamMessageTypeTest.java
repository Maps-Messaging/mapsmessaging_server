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

package io.mapsmessaging.network.protocol.impl.amqp.jms.messages;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import org.junit.jupiter.api.Assertions;

public class StreamMessageTypeTest extends MessageTypeTest {

  @Override
  protected Message getMessage(Session session) throws JMSException {
    StreamMessage streamMessage = session.createStreamMessage();
    streamMessage.setIntProperty("IntProperty", 4);
    streamMessage.setLongProperty("LongProperty", 1 << 16);
    streamMessage.setStringProperty("StringProperty", "This is a string property");
    streamMessage.writeString("This Should be a stream message");
    streamMessage.writeString("This Should be the second stream message");
    streamMessage.writeInt(2);
    streamMessage.writeLong(2L << 32);
    return streamMessage;
  }

  @Override
  protected void testMessage(Message message) throws JMSException {
    Assertions.assertNotNull(message);
    Assertions.assertTrue(message instanceof StreamMessage);
    StreamMessage message1 = (StreamMessage) message;
    Assertions.assertEquals(4, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1 << 16, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
    Assertions.assertEquals("This Should be a stream message", message1.readString());
    Assertions.assertEquals("This Should be the second stream message", message1.readString());
    Assertions.assertEquals(2, message1.readInt());
    Assertions.assertEquals(2L << 32, message1.readLong());
  }
}
