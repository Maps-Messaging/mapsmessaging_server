/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.amqp.client;

import io.mapsmessaging.test.BaseTestConfig;
import jakarta.jms.*;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

class StandardClientTest extends BaseTestConfig {


  @Test
  void simpleConnection() throws IOException, JMSException, NamingException {
    Hashtable<Object, Object> env = new Hashtable<Object, Object>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
    env.put("destination.topicExchange", "amqp.topic");
    env.put("fred", "amqp.topic");
    javax.naming.Context context = new javax.naming.InitialContext(env);

// Example connection URL with SASL PLAIN (replace with your server details)
// Create a JMS Connection Factory
    JmsConnectionFactory factory = new JmsConnectionFactory();

// Set the connection properties
    factory.setRemoteURI("amqp://localhost:5673/localhost");


// You can specify the SASL mechanism if needed
    factory.setUsername("guest");
    factory.setPassword(getPassword("guest"));

// Create and start the JMS connection
    Connection connection = factory.createConnection();
    connection.start();
    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    Destination destination = (Destination) context.lookup("dynamicTopics");
    MessageProducer messageProducer = session.createProducer(destination);
    MessageConsumer messageConsumer = session.createConsumer(destination);

    TextMessage message = session.createTextMessage("Hello world!");
    messageProducer.send(message);

    message = (TextMessage) messageConsumer.receive();
    System.out.println(message.getText());
    connection.close();
  }

  @Test
  void qpidTest() {
    try {
      Properties properties = new Properties();
      properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
      properties.put("connectionfactory.qpidConnectionfactory", "amqp://localhost::5673/test?brokerlist='tcp://localhost:5673'");
      properties.put("destination.topicExchange", "amq.topic");

      Context context = new InitialContext(properties);

      ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
      //connectionFactory.setUsername("admin");
      //factory.setPassword(getPassword("admin"));

      Connection connection = connectionFactory.createConnection();
      connection.start();

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination destination = (Destination) context.lookup("topicExchange");

      MessageProducer messageProducer = session.createProducer(destination);
      MessageConsumer messageConsumer = session.createConsumer(destination);

      TextMessage message = session.createTextMessage("Hello world!");
      messageProducer.send(message);

      message = (TextMessage) messageConsumer.receive();
      System.out.println(message.getText());

      connection.close();
      context.close();
    } catch (Exception exp) {
      exp.printStackTrace();
    }
  }
}
