/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.amqp.jms;

import java.io.IOException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MessageTypeTest extends BaseConnection {


  @Test
  void sendAllMessageTypeTest() throws IOException, NamingException, JMSException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Assertions.assertNotNull(connectionFactory);
    Connection connection = connectionFactory.createConnection();
    Assertions.assertNotNull(connection);
    connection.start();

    Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
    Destination destination = (Destination) context.lookup("topicExchange");

    MessageConsumer messageConsumer = session.createConsumer(destination);
    MessageProducer messageProducer = session.createProducer(destination);

    String payload = "Hello world! this should be a byte []";
    Message message1;
    /*
    Message message = session.createMessage();
    message.setIntProperty("IntProperty", 100);
    message.setLongProperty("LongProperty", 1L << 32);
    message.setStringProperty("StringProperty", "This is a generic message property");
    messageProducer.send(message);

    message1 = messageConsumer.receive(1000);
    Assertions.assertNotNull(message1);
    Assertions.assertEquals(100, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1L << 32, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a generic message property", message1.getStringProperty("StringProperty"));

    TextMessage textMessage = session.createTextMessage("Hello world! this should be a text message");
    textMessage.setIntProperty("IntProperty", 1);
    textMessage.setLongProperty("LongProperty", 1 << 2);
    textMessage.setStringProperty("StringProperty", "This is a string property");
    messageProducer.send(textMessage);
    message1 = messageConsumer.receive(2000);
    Assertions.assertNotNull(message1);
    Assertions.assertTrue(message1 instanceof TextMessage);
    Assertions.assertEquals(1, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1 << 2, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));

    BytesMessage bytesMessage = session.createBytesMessage();
    bytesMessage.setIntProperty("IntProperty", 2);
    bytesMessage.setLongProperty("LongProperty", 1 << 8);
    bytesMessage.setStringProperty("StringProperty", "This is a string property");
    bytesMessage.writeBytes(payload.getBytes());
    messageProducer.send(bytesMessage);
    message1 = messageConsumer.receive(2000);
    Assertions.assertNotNull(message1);
    Assertions.assertTrue(message1 instanceof BytesMessage);
    Assertions.assertEquals(2, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1 << 8, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
    byte[] test = new byte[payload.length()];
    ((BytesMessage) message1).readBytes(test);
    Assertions.assertEquals(payload, new String(test));

*/
    MapMessage mapMessage = session.createMapMessage();
    mapMessage.setIntProperty("IntProperty", 3);
    mapMessage.setLongProperty("LongProperty", 1 << 10);
    mapMessage.setStringProperty("StringProperty", "This is a string property");
    mapMessage.setBytes("bytesProperty", new byte[100]);
    mapMessage.setString("body", payload);
    messageProducer.send(mapMessage);
    message1 = messageConsumer.receive(2000);
    Assertions.assertNotNull(message1);
    Assertions.assertTrue(message1 instanceof MapMessage);
    Assertions.assertEquals(3, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1 << 10, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
    Assertions.assertEquals(payload, mapMessage.getString("body"));

    /*
    StreamMessage streamMessage = session.createStreamMessage();
    streamMessage.setIntProperty("IntProperty", 4);
    streamMessage.setLongProperty("LongProperty", 1 << 16);
    streamMessage.setStringProperty("StringProperty", "This is a string property");
    streamMessage.writeString("This Should be a stream message");
    streamMessage.writeString("This Should be the second stream message");
    streamMessage.writeInt(2);
    streamMessage.writeLong(2L<<32);
    messageProducer.send(streamMessage);
    message1 = messageConsumer.receive(1000);
    Assertions.assertNotNull(message1);
    Assertions.assertTrue(message1 instanceof StreamMessage);
    Assertions.assertEquals(4, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1 << 16, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
    Assertions.assertEquals("This Should be a stream message", ((StreamMessage) message1).readString());
    Assertions.assertEquals("This Should be the second stream message", ((StreamMessage) message1).readString());
    Assertions.assertEquals(2, ((StreamMessage) message1).readInt());
    Assertions.assertEquals(2L<<32, ((StreamMessage) message1).readLong());

    ObjectMessage objectMessage = session.createObjectMessage();
    objectMessage.setIntProperty("IntProperty", 5);
    objectMessage.setLongProperty("LongProperty", 1L << 32);
    objectMessage.setStringProperty("StringProperty", "This is a string property");
    objectMessage.setObject("This Should be a object message of a string");
    messageProducer.send(objectMessage);
    message1 = messageConsumer.receive(1000);
    Assertions.assertNotNull(message1);
    Assertions.assertTrue(message1 instanceof ObjectMessage);
    Assertions.assertEquals(5, message1.getIntProperty("IntProperty"));
    Assertions.assertEquals(1L << 32, message1.getLongProperty("LongProperty"));
    Assertions.assertEquals("This is a string property", message1.getStringProperty("StringProperty"));
    Object result = ((ObjectMessage) message1).getObject();
    Assertions.assertTrue(result instanceof String);
    Assertions.assertEquals("This Should be a object message of a string", result);
*/
    messageProducer.close();
    messageConsumer.close();
    connection.close();
    context.close();
  }

}
