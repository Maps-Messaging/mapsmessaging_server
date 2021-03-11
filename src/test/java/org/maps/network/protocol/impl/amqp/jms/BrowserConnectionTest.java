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
import java.util.Enumeration;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BrowserConnectionTest extends BaseConnection {

  @Test
  void simpleBrowserTest() throws JMSException, NamingException, IOException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Assertions.assertNotNull(connectionFactory);
    try (Connection connection = connectionFactory.createConnection()) {
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue("browseQueue");
      MessageProducer producer = session.createProducer(queue);
      String task = "Task";
      for (int i = 0; i < 10; i++) {
        String payload = task + i;
        Message msg = session.createTextMessage(payload);
        System.out.println("Sending text '" + payload + "'");
        producer.send(msg);
      }

      MessageConsumer consumer = session.createConsumer(queue);
      connection.start();

      System.out.println("Browse through the elements in queue");
      QueueBrowser browser = session.createBrowser(queue);
      int counter =0;
      Enumeration e = browser.getEnumeration();
      while (e.hasMoreElements()) {
        TextMessage message = (TextMessage) e.nextElement();
        System.out.println("Browse [" + message.getText() + "]");
        counter++;
      }
      Assertions.assertEquals(10, counter, "We pushed 10 messages, we expect the browser to see 10 messages");
      System.out.println("Done");
      browser.close();


      for (int i = 0; i < 10; i++) {
        TextMessage textMsg = (TextMessage) consumer.receive();
        Assertions.assertNotNull(textMsg, "We now expect the consumer to receive the messages sent since the browser is read only");
        System.out.println(textMsg);
        System.out.println("Received: " + textMsg.getText());
      }
      session.close();
    }
  }

  @Test
  void simpleFilterBrowserTest() throws JMSException, NamingException, IOException {
    Context context = loadContext();
    Assertions.assertNotNull(context);

    ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
    Assertions.assertNotNull(connectionFactory);
    try (Connection connection = connectionFactory.createConnection()) {
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue("filteredQueue");
      MessageProducer producer = session.createProducer(queue);
      String task = "Task";
      for (int i = 0; i < 10; i++) {
        String payload = task + i;
        Message msg = session.createTextMessage(payload);
        msg.setBooleanProperty("odd", i%2==1);
        System.out.println("Sending text '" + payload + "'");
        producer.send(msg);
      }

      MessageConsumer consumer = session.createConsumer(queue);
      connection.start();

      System.out.println("Browse through the elements in queue");
      QueueBrowser browser = session.createBrowser(queue, "odd = true");
      Enumeration e = browser.getEnumeration();
      int counter =0;
      while (e.hasMoreElements()) {
        TextMessage message = (TextMessage) e.nextElement();
        System.out.println("Browse [" + message.getText() + "]");
        counter++;
      }
      Assertions.assertEquals(5, counter, "We expect 1/2 the messages since now we are filtering it by odd/even");
      System.out.println("Done");
      browser.close();

      for (int i = 0; i < 10; i++) {
        TextMessage textMsg = (TextMessage) consumer.receive();
        Assertions.assertNotNull(textMsg, "We now expect the consumer to receive the messages sent since the browser is read only");
        System.out.println(textMsg);
        System.out.println("Received: " + textMsg.getText());
      }
      session.close();
    }
  }
}
