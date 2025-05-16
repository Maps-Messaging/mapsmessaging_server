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

package io.mapsmessaging.network.protocol.impl.amqp.jms;

import io.mapsmessaging.engine.session.SessionManagerTest;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.TextMessage;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TemporaryDestinationTest extends BaseConnection {

  @Test
  void simpleTemporaryTopicTest() throws JMSException, NamingException, IOException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Assertions.assertNotNull(connectionFactory);
    Connection connection = connectionFactory.createConnection();
    Assertions.assertNotNull(connection);
    connection.start();

    Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
    TemporaryTopic topic = session.createTemporaryTopic();
    MessageConsumer consumer = session.createConsumer(topic);
    Assertions.assertNotNull(consumer);

    MessageProducer messageProducer = session.createProducer(topic);
    Assertions.assertNotNull(messageProducer);

    for (int x = 0; x < 60; x++) {
      TextMessage message = session.createTextMessage("Hello world! this should be a queue");
      message.setIntProperty("IntProperty", 1);
      message.setLongProperty("LongProperty", 1 << 10);
      message.setStringProperty("StringProperty", "This is a string property");
      messageProducer.send(message);
    }

    int count = 0;
    while(true){
      Message message = consumer.receive(1000);
      if(message == null){
        break;
      }
      count++;
    }

    Assertions.assertEquals(60, count);
    messageProducer.close();
    consumer.close();
    topic.delete();
    session.close();
    connection.close();
    count = 0;
    while(SessionManagerTest.getInstance().hasIdleSessions() && count < 100){
      delay(1);
      count++;
    }
  }

  @Test
  void simpleTemporaryQueueTest() throws JMSException, NamingException, IOException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Assertions.assertNotNull(connectionFactory);
    Connection connection = connectionFactory.createConnection();
    Assertions.assertNotNull(connection);
    connection.start();

    Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
    TemporaryQueue queue = session.createTemporaryQueue();
    MessageConsumer consumer = session.createConsumer(queue);
    Assertions.assertNotNull(consumer);

    MessageProducer messageProducer = session.createProducer(queue);
    Assertions.assertNotNull(messageProducer);

    for (int x = 0; x < 60; x++) {
      TextMessage message = session.createTextMessage("Hello world! this should be a queue");
      message.setIntProperty("IntProperty", 1);
      message.setLongProperty("LongProperty", 1 << 10);
      message.setStringProperty("StringProperty", "This is a string property");
      messageProducer.send(message);
    }

    int count = 0;
    while(true){
      Message message = consumer.receive(1000);
      if(message == null){
        break;
      }
      count++;
    }

    Assertions.assertEquals(60, count);
    messageProducer.close();
    consumer.close();
    queue.delete();
    session.close();
    connection.close();
    count = 0;
    while(SessionManagerTest.getInstance().hasIdleSessions() && count < 100){
      delay(1);
      count++;
    }
  }

}