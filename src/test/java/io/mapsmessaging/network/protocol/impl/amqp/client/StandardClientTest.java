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

package io.mapsmessaging.network.protocol.impl.amqp.client;

import io.mapsmessaging.test.BaseTestConfig;
import jakarta.jms.*;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StandardClientTest extends BaseTestConfig {



  @Test
  void qpidTest() throws Exception {
    Properties properties = new Properties();
    properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
    properties.put("connectionfactory.qpidConnectionfactory", "amqp://localhost:5673/test?brokerlist='tcp://localhost:5673'");
    properties.put("destination.topicExchange", "amq.topic");

    Context context = new InitialContext(properties);
    Connection connection = ((ConnectionFactory) context.lookup("qpidConnectionfactory")).createConnection();
    try {
      connection.start();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination destination = (Destination) context.lookup("topicExchange");
      MessageProducer messageProducer = session.createProducer(destination);
      MessageConsumer messageConsumer = session.createConsumer(destination);
      messageProducer.send(session.createTextMessage("Hello world!"));

      TextMessage message = (TextMessage) messageConsumer.receive(2000);

      Assertions.assertNotNull(message);
      Assertions.assertEquals("Hello world!", message.getText());
    } finally {
      connection.close();
      context.close();
    }
  }
}
