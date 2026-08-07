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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.AmqpConfigDTO;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.EventListenerFactory;
import io.mapsmessaging.network.protocol.impl.amqp.proton.tasks.InputPacketProcessTask;
import io.mapsmessaging.network.protocol.impl.amqp.proton.tasks.SendMessageTask;
import io.mapsmessaging.network.protocol.impl.amqp.proton.tasks.TickTask;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final AmqpTransactionCoordinator transactionCoordinator;

  @Getter
  private final SaslManager saslManager;

  @Getter
  private final Connection connection;
  private final TaskScheduler engineScheduler;
  private final AtomicBoolean closed;
  private volatile ScheduledFuture<?> tickFuture;

  public ProtonEngine(AMQPProtocol protocol) throws IOException {
    engineScheduler = new SingleConcurrentTaskScheduler(PROTON_ENGINE_KEY);
    this.protocol = protocol;
    collector = Collector.Factory.create();
    connection = Connection.Factory.create();
    connection.collect(collector);
    transport = Transport.Factory.create();
    AmqpConfigDTO config = protocol.getAmqpConfig();
    transport.setMaxFrameSize(config.getMaxFrameSize());
    transport.setOutboundFrameSizeLimit(config.getMaxFrameSize());
    transport.setIdleTimeout(config.getIdleTimeout());
    subscriptions = new SubscriptionManager();
    transactionCoordinator = new AmqpTransactionCoordinator();
    eventListenerFactory = new EventListenerFactory(protocol, this);
    saslManager = new SaslManager(this);
    closed = new AtomicBoolean(false);
    tickFuture = null;
    if (saslManager.isDone()) {
      transport.bind(connection);
    }
  }

  public void close() throws IOException {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    if (tickFuture != null) {
      tickFuture.cancel(false);
    }
    IOException failure = null;
    try {
      transport.close();
      connection.close();
      transactionCoordinator.close();
    } catch (IOException e) {
      failure = e;
    } finally {
      subscriptions.close();
      engineScheduler.shutdown();
    }
    if (failure != null) {
      throw failure;
    }
  }

  public void processPacket(Packet packet) throws IOException {
    if (closed.get()) {
      throw new IOException("AMQP engine is closed");
    }
    Future<Boolean> future = engineScheduler.submit(new InputPacketProcessTask(this, packet));
    try {
      future.get(10000, TimeUnit.MILLISECONDS);
      tick();
    } catch (InterruptedException e) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while processing AMQP input", e);
    } catch (ExecutionException | TimeoutException e) {
      future.cancel(true);
      throw new IOException(e);
    }
  }

  public void sendMessage(Message message, SubscribedEventManager manager, Runnable completionTask) {
    if (closed.get()) {
      manager.rollbackReceived(message.getIdentifier());
      return;
    }
    engineScheduler.submit(() -> {
      if (closed.get()) {
        manager.rollbackReceived(message.getIdentifier());
        return false;
      }
      try {
        new SendMessageTask(this, message, manager).call();
        if (completionTask != null) {
          completionTask.run();
        }
      } catch (Exception e) {
        protocol.getLogger().log(ServerLogMessages.AMQP_ENGINE_TRANSPORT_EXCEPTION, e);
        manager.rollbackReceived(message.getIdentifier());
      }
      return true;
    });
  }

  public void sendMessage(Message message, SubscribedEventManager manager) {
    sendMessage(message, manager, null);
  }

  public void tick() {
    if (closed.get()) {
      return;
    }
    engineScheduler.submit(() -> {
      if (closed.get()) {
        return false;
      }
      try {
        return new TickTask(this).call();
      } catch (Exception e) {
        try {
          protocol.close();
        } catch (IOException closeException) {
          e.addSuppressed(closeException);
        }
        protocol.getLogger().log(ServerLogMessages.AMQP_ENGINE_TRANSPORT_EXCEPTION, e);
        return false;
      }
    });
  }

  public void scheduleTick(long deadline, long now) {
    if (closed.get()) {
      return;
    }
    if (tickFuture != null) {
      tickFuture.cancel(false);
      tickFuture = null;
    }
    if (deadline != 0) {
      long delay = Math.max(1, deadline - now);
      tickFuture = SimpleTaskScheduler.getInstance().schedule(this::tick, delay, TimeUnit.MILLISECONDS);
    }
  }
}
