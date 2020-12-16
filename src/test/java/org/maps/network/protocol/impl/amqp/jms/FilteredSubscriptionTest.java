/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.amqp.jms;

import java.io.IOException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FilteredSubscriptionTest extends BaseConnection {

  @Test
  void simpleFilteredTest() throws JMSException, NamingException, IOException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Assertions.assertNotNull(connectionFactory);
    Connection connection = connectionFactory.createConnection();
    Assertions.assertNotNull(connection);
    connection.start();

    Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
    Topic topic = (Topic) context.lookup("topicExchange");
    MessageConsumer consumer = session.createConsumer(topic, "Country in ('France')");
    Assertions.assertNotNull(consumer);

    MessageProducer messageProducer = session.createProducer(topic);
    Assertions.assertNotNull(messageProducer);

    for (int x = 0; x < 60; x++) {
      TextMessage message = session.createTextMessage("Hello world! this should be a queue");
      message.setIntProperty("IntProperty", 1);
      message.setLongProperty("LongProperty", 1 << 10);
      message.setStringProperty("StringProperty", "This is a string property");
      if(x%2 == 0) {
        message.setStringProperty("key", "a");
      }
      else{
        message.setStringProperty("key", "b");
      }
      message.setStringProperty("Country", "France");
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

    // Should only get 1/2 the events
    Assertions.assertEquals(60, count);
    messageProducer.close();
    consumer.close();
    session.close();
    connection.close();
  }

}