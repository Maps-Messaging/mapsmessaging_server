/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.MessageProducer;
import jakarta.jms.Message;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleTransactionConnectionTest extends BaseConnection {

  @Test
  void simpleTopicPubSub() throws JMSException, NamingException, IOException {
    runSub(Session.SESSION_TRANSACTED,"qpidConnectionfactory", "topicExchange");
  }

  @Test
  void simpleSharedPubSub() throws JMSException, NamingException, IOException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Assertions.assertNotNull(connectionFactory);
    Connection connection = connectionFactory.createConnection();
    Assertions.assertNotNull(connection);
    connection.start();

    Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
    Topic topic = (Topic) context.lookup("topicExchange");
    MessageConsumer shared = session.createSharedConsumer(topic, "NameOfDurable");
    Assertions.assertNotNull(shared);

    int sent = sendEvents(session, topic);
    session.commit();

    int received = receiveMessages(shared);
    session.commit();

    Assertions.assertEquals(sent, received);
    shared.close();
    connection.close();
    context.close();
  }

  @Test
  void simpleQueuePubSub()  throws JMSException, NamingException, IOException {
    runSub(Session.SESSION_TRANSACTED,"qpidConnectionfactory", "queueExchange");
  }

  @Test
  void rollback_restores_published_and_consumed_messages() throws JMSException, NamingException, IOException {
    Context context = loadContext();
    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Connection connection = connectionFactory.createConnection();
    connection.start();

    Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
    TemporaryQueue queue = session.createTemporaryQueue();
    MessageProducer producer = session.createProducer(queue);
    MessageConsumer consumer = session.createConsumer(queue);

    producer.send(session.createTextMessage("rolled back publish"));
    session.rollback();
    Assertions.assertNull(consumer.receive(500));

    producer.send(session.createTextMessage("committed publish"));
    session.commit();
    Message firstDelivery = consumer.receive(2000);
    Assertions.assertNotNull(firstDelivery);
    session.rollback();

    Message redelivery = consumer.receive(2000);
    Assertions.assertNotNull(redelivery);
    Assertions.assertTrue(redelivery.getJMSRedelivered());
    session.commit();

    consumer.close();
    producer.close();
    queue.delete();
    session.close();
    connection.close();
    context.close();
  }
}
