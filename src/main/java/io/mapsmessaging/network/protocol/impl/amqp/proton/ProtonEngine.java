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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.EventListenerFactory;
import io.mapsmessaging.network.protocol.impl.amqp.proton.tasks.InputPacketProcessTask;
import io.mapsmessaging.network.protocol.impl.amqp.proton.tasks.SendMessageTask;
import io.mapsmessaging.utilities.threads.tasks.SingleConcurrentTaskScheduler;
import io.mapsmessaging.utilities.threads.tasks.TaskScheduler;
import lombok.Getter;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Transport;

import java.io.IOException;
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
  private final SubscriptionManager subscriptions;

  @Getter
  private final SaslManager saslManager;

  @Getter
  private final Connection connection;
  private TaskScheduler engineScheduler;

  public ProtonEngine(AMQPProtocol protocol) throws IOException {
    engineScheduler = new SingleConcurrentTaskScheduler(PROTON_ENGINE_KEY);
    this.protocol = protocol;
    collector = Collector.Factory.create();
    connection = Connection.Factory.create();
    connection.collect(collector);
    transport = Transport.Factory.create();
    subscriptions = new SubscriptionManager();
    eventListenerFactory = new EventListenerFactory(protocol, this);
    saslManager = new SaslManager(this);
    if (saslManager.isDone()) {
      transport.bind(connection);
    }
  }

  public void close() {
    transport.close();
    connection.close();
    subscriptions.close();
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
}
