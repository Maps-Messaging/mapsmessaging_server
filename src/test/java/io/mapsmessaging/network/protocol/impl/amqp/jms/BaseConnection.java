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

import io.mapsmessaging.test.BaseTestConfig;
import io.mapsmessaging.utilities.ResourceList;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;

public class BaseConnection extends BaseTestConfig {

  protected void runSub(int mode, String factoryName, String destinationName) throws IOException, NamingException, JMSException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(factoryName);
    Assertions.assertNotNull(connectionFactory);
    Connection connection = connectionFactory.createConnection();
    Assertions.assertNotNull(connection);
    connection.start();

    Session session = connection.createSession(mode);
    Destination destination = (Destination) context.lookup(destinationName);

    MessageConsumer messageConsumer = session.createConsumer(destination);
    int loop = 1;
    if(mode == Session.SESSION_TRANSACTED){
      loop = 4;
    }
    for(int x=0;x<loop;x++) {
      int sent = sendEvents(session, destination);
      if (mode == Session.SESSION_TRANSACTED) {
        session.commit();
      }
      int received = receiveMessages(messageConsumer);
      if (mode == Session.SESSION_TRANSACTED) {
        session.commit();
      }
      Assertions.assertEquals(sent, received);
    }
    messageConsumer.close();
    connection.close();
    context.close();
  }

  protected void runDurableSub(int mode, String factoryName, String destinationName, String clientId, String durable) throws IOException, NamingException, JMSException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(factoryName);
    Assertions.assertNotNull(connectionFactory);
    Connection connection = connectionFactory.createConnection();
    Assertions.assertNotNull(connection);
    connection.setClientID(clientId);
    Assertions.assertEquals(clientId, connection.getClientID());
    connection.start();

    Session session = connection.createSession(mode);
    Destination destination = (Destination) context.lookup(destinationName);
    if(!(destination instanceof Topic)){
      throw new IOException("Expected topic but found "+destination);
    }

    MessageConsumer messageConsumer = session.createDurableConsumer((Topic) destination, durable);
    int sent = sendEvents(session, destination);
    int received = receiveMessages(messageConsumer);
    Assertions.assertEquals(sent, received);
    messageConsumer.close();
    connection.close();
    context.close();
  }

  protected int sendEvents(Session session, Destination destination) throws JMSException {
    int counter =0;
    for (int y = 0; y < 10; y++) {
      MessageProducer messageProducer = session.createProducer(destination);
      for (int x = 0; x < 60; x++) {
        TextMessage message = session.createTextMessage("Hello world! this should be a queue");
        message.setIntProperty("IntProperty", 1);
        message.setLongProperty("LongProperty", 1 << 10);
        message.setStringProperty("StringProperty", "This is a string property");
        messageProducer.send(message);
        counter++;
      }
      messageProducer.close();
    }
    return counter;
  }

  protected int receiveMessages(MessageConsumer consumer){
    TextMessage message;
    int counter = 0;
    while (true) {
      try {
        message = (TextMessage) consumer.receive(2000);
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
    return counter;
  }

  protected Context loadContext() throws IOException, NamingException {
    Properties properties = new Properties();
    Collection<String> knownProperties = ResourceList.getResources(Pattern.compile(".*simpleConnectionTest.props"));
    Assertions.assertNotNull(knownProperties);
    for (String config : knownProperties) {
      try (FileInputStream fis = new FileInputStream(config)) {
        properties.load(fis);
      }
    }
    return new InitialContext(properties);
  }

}
