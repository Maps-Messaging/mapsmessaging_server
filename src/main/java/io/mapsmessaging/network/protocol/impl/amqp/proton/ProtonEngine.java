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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.EventListenerFactory;
import io.mapsmessaging.network.protocol.impl.amqp.proton.tasks.InputPacketProcessTask;
import io.mapsmessaging.network.protocol.impl.amqp.proton.tasks.SendMessageTask;
import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;
import io.mapsmessaging.utilities.threads.tasks.SingleConcurrentTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.TaskScheduler;
import lombok.Getter;
import org.apache.qpid.proton.engine.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProtonEngine {

  private static final String PROTON_ENGINE_KEY = "ProtonEngineScheduler";

  @Getter
  private final AMQPProtocol protocol;
  @Getter
  private final Transport transport;
  @Getter
  private final Collector collector;
  @Getter
  private final EventListenerFactory eventListenerFactory;
  @Getter
  private final Map<String, Sender> subscriptions;

  private final Connection connection;
  private TaskScheduler engineScheduler;

  private final SaslAuthenticationMechanism saslContext;
  private final Sasl sasl;

  public ProtonEngine(AMQPProtocol protocol) throws IOException {
    engineScheduler = new SingleConcurrentTaskScheduler(PROTON_ENGINE_KEY);

    this.protocol = protocol;
    collector = Collector.Factory.create();
    connection = Connection.Factory.create();
    connection.collect(collector);
    transport = Transport.Factory.create();
    subscriptions = new LinkedHashMap<>();
    eventListenerFactory = new EventListenerFactory(protocol, this);
    saslContext = protocol.getAuthenticationContext();
    sasl = transport.sasl();
    sasl.setMechanisms("ANONYMOUS");
    sasl.server();
    sasl.done(Sasl.PN_SASL_OK);
    transport.bind(connection);
  }

  public void close() {
    transport.close();
    connection.close();
    for (Entry<String, Sender> entry : subscriptions.entrySet()) {
      Object sessionContext = entry.getValue().getSession().getContext();
      if (sessionContext != null) {
        Session session = (Session) sessionContext;
        session.removeSubscription(entry.getKey());
      }
    }
    subscriptions.clear();
    engineScheduler = null;
  }

  public void processPacket(Packet packet) throws IOException {
    Future<Boolean> future = engineScheduler.submit(new InputPacketProcessTask(this, packet));
    try {
      if (!future.isDone()) {
        future.get(10000, TimeUnit.MILLISECONDS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException e) {
      throw new IOException(e);
    }
  }

  public void sendMessage(Message message, SubscribedEventManager manager) {
    engineScheduler.submit(new SendMessageTask(this, message, manager));
  }

  public long unpackLong(byte[] buff) {
    long value = 0;
    for (int x = 0; x < buff.length; x++) {
      long val = buff[x];
      value ^= (val & 0xff) << (8 * x);
    }
    return value;
  }


  public void addSubscription(String alias, Sender sender) {
    synchronized (subscriptions) {
      subscriptions.put(alias, sender);
    }
  }

  public void removeSubscription(String alias) {
    synchronized (subscriptions) {
      subscriptions.remove(alias);
    }
  }
}
