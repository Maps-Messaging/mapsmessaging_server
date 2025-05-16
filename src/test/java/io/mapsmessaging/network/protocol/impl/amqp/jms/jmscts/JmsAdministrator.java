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

package io.mapsmessaging.network.protocol.impl.amqp.jms.jmscts;

import io.mapsmessaging.utilities.ResourceList;
import jakarta.jms.JMSException;
import jakarta.jms.XATopicConnectionFactory;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import org.junit.jupiter.api.Assertions;

public class JmsAdministrator {//implements Administrator {

  private final NamingException ex;
  private Context context;
  private  Properties properties = new Properties();

  public JmsAdministrator(){
    System.err.println("JMS Admin starting");
    NamingException error = null;
    Context con = null;
    try {
      Collection<String> knownProperties = ResourceList.getResources(Pattern.compile(".*simpleConnectionTest.props"));
      Assertions.assertNotNull(knownProperties);
      for (String config : knownProperties) {
        try (FileInputStream fis = new FileInputStream(config)) {
          properties.load(fis);
        }
      }
      con = new InitialContext(properties);
    } catch (Exception ex) {
      ex.printStackTrace();
      error = new NamingException(ex.getMessage());
    }
    context = con;
    ex = error;
  }

  /**
   * Returns the name of the QueueConnectionFactory bound in JNDI
   *
   * @return the default QueueConnectionFactory name
   */
  public String getQueueConnectionFactory() {
    return "QueueConnectionFactory";
  }

  /**
   * Returns the name of the TopicConnectionFactory bound in JNDI
   *
   * @return the default TopicConnectionFactory name
   */
  public String getTopicConnectionFactory() {
    return "TopicConnectionFactory";
  }

  public String getXAQueueConnectionFactory() {
    return "XAQueueConnectionFactory";
  }

  public String getXATopicConnectionFactory() {
    XATopicConnectionFactory factory;
    return "XAQTopicConnectionFactory";
  }

  public Object lookup(String s) throws NamingException {
    return getContext().lookup(s);
  }

  /**
   * Returns the JNDI context used to look up administered objects
   *
   * @return the JNDI context under which administered objects are bound
   * @throws NamingException if the context cannot be obtained
   */
  public Context getContext() throws NamingException {
    if(context != null){
      return context;
    }
    throw ex;
  }

  /**
   * Create an administered destination
   *
   * @param name the destination name
   * @param queue if true, create a queue, else create a topic
   * @throws JMSException if the destination cannot be created
   */
  public void createDestination(String name, boolean queue) throws JMSException {
    try {
      if(queue){
        properties.put("queue."+name, "amqp.queue");
      }
      else{
        properties.put("topic."+name, "amqp.topic");
      }
      context = new InitialContext(properties);
    } catch (NamingException e) {
      e.printStackTrace();
      JMSException jmsEx = new JMSException(e.getMessage());
      jmsEx.initCause(e);
      throw jmsEx;
    }
  }

  /**
   * Destroy an administered destination
   *
   * @param name the destination name
   * @throws JMSException if the destination cannot be destroyed
   */
  public void destroyDestination(String name) throws JMSException {
  }

  /**
   * Returns true if an administered destination exists
   *
   * @param name the destination name
   * @throws JMSException for any internal JMS provider error
   */
  public boolean destinationExists(String name)
      throws JMSException {

    boolean exists = false;
    try {
      getContext().lookup(name);
      exists = true;
    } catch (NameNotFoundException ignore) {
    } catch (Exception exception) {
      JMSException error = new JMSException(exception.getMessage());
      error.setLinkedException(exception);
      throw error;
    }
    return exists;
  }
}