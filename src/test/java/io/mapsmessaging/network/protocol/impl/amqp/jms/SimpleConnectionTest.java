/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.amqp.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleConnectionTest extends BaseConnection {

  @Test
  void simpleTopicPubSub() throws JMSException, NamingException, IOException {
    runSub(Session.AUTO_ACKNOWLEDGE, "qpidConnectionfactory", "topicExchange");
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

    Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
    Topic topic = (Topic) context.lookup("topicExchange");
    MessageConsumer shared = session.createSharedConsumer(topic, "NameOfDurable");
    Assertions.assertNotNull(shared);

    int sent = sendEvents(session, topic);
    int received = receiveMessages(shared);
    Assertions.assertEquals(sent, received);
    shared.close();
    connection.close();
    context.close();
  }

  @Test
  void simpleQueuePubSub()  throws JMSException, NamingException, IOException {
    runSub(Session.AUTO_ACKNOWLEDGE, "qpidConnectionfactory", "queueExchange");
  }

}

