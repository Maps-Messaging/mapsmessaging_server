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

import io.mapsmessaging.network.protocol.impl.amqp.jms.BaseConnection;
import jakarta.jms.*;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

abstract class MessageTypeTest extends BaseConnection {

  protected static final String payload = "Hello world! this should be a byte []";


  abstract protected Message getMessage(Session session) throws JMSException;

  abstract protected void testMessage(Message message) throws JMSException;

  @Test
  void validateMessage() throws IOException, NamingException, JMSException {
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

    Message message = getMessage(session);
    messageProducer.send(message);
    testMessage(messageConsumer.receive(1000));

    messageProducer.close();
    messageConsumer.close();
    connection.close();
    context.close();
  }

}
