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

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleDurableConnectionTest extends BaseConnection {

  @Test
  void simpleDurableTopicPubSub() throws JMSException, NamingException, IOException, InterruptedException {
    runDurableSub( Session.AUTO_ACKNOWLEDGE,"qpidConnectionfactory", "topicExchange", "UniqueClientId","DurableName");
  }

  @Test
  void simpleSharedDurablePubSub() throws JMSException, NamingException, IOException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Assertions.assertNotNull(connectionFactory);
    Connection connection = connectionFactory.createConnection();
    Assertions.assertNotNull(connection);
    connection.start();

    Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
    Topic topic = (Topic) context.lookup("topicExchange");
    MessageConsumer shared = session.createSharedDurableConsumer(topic, "NameOfDurable");
    Assertions.assertNotNull(shared);

    int sent = sendEvents(session, topic);

    TextMessage message;
    int counter = 0;
    while (true) {
      try {
        message = (TextMessage) shared.receive(10);
        if (message != null) {
          message.acknowledge();
          counter++;
        } else {
          break;
        }
      } catch (JMSException e) {
        Assertions.fail(e);
        break;
      }
    }

    Assertions.assertEquals(sent, counter);
    try {
      shared.close();
      connection.close();
      context.close();
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }
}
