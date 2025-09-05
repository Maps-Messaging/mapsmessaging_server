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

package io.mapsmessaging.engine.session.will;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WillTaskImpl implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(WillTaskImpl.class);

  private final WillDetails details;
  private boolean active;
  private Future<?> scheduledFuture;

  WillTaskImpl(WillDetails details) {
    this.details = details;
    active = true;
    scheduledFuture = null;
  }

  public void cancel() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
    WillTaskManager.getInstance().remove(details.getSessionId());
  }

  public void clear() {
    active = false;
    if (!scheduledFuture.isDone()) {
      scheduledFuture.cancel(false);
    }
  }

  public void schedule() {
    if (details.getDelay() > 0) {
      scheduledFuture = SimpleTaskScheduler.getInstance().schedule(this, details.getDelay(), TimeUnit.SECONDS);
    } else {
      run();
    }
  }

  @SneakyThrows
  @Override
  public void run() {
    try {
      if (active) {
        ThreadContext.put("session", details.getSessionId());
        ThreadContext.put("protocol", details.getProtocol());
        ThreadContext.put("version", details.getVersion());
        logger.log(ServerLogMessages.WILL_TASK_SENDING, details.getMsg());
        CompletableFuture<DestinationImpl> future = MessageDaemon.getInstance().getDestinationManager().find(details.getDestination());
        DestinationImpl dest = future.get();
        if (dest != null) {
          dest.storeMessage(details.getMsg());
        }
      }
      WillTaskManager.getInstance().remove(details.getSessionId());
    } catch (IOException e) {
      logger.log(ServerLogMessages.WILL_TASK_EXCEPTION, e, details.getMsg().toString());
    } finally {
      ThreadContext.clearMap();
    }
  }

  public void updateTopic(String topic) {
    details.updateTopic(topic);
    updateManager();
  }

  public void updateQoS(QualityOfService qos) {
    Message previous = details.getMsg();
    MessageBuilder messageBuilder = new MessageBuilder();
    if (previous != null) {
      messageBuilder = new MessageBuilder(previous);
    }
    messageBuilder.setQoS(qos);
    details.updateMessage(messageBuilder.build());
    updateManager();
  }

  public void updateMessage(byte[] payload) {
    MessageBuilder messageBuilder = new MessageBuilder(details.getMsg());
    messageBuilder.setOpaqueData(payload);
    details.updateMessage(messageBuilder.build());
    updateManager();
  }

  void updateManager() {
    if (details.getSessionId() != null) {
      WillTaskManager.getInstance().remove(details.getSessionId());
      WillTaskManager.getInstance().put(details.getSessionId(), details);
    }
  }

  @Override
  public String toString() {
    return "WillTask:Active:" + active + " " + details.toString();
  }

  public void updateRetain(boolean flag) {
    Message previous = details.getMsg();
    MessageBuilder messageBuilder = new MessageBuilder();
    if (previous != null) {
      messageBuilder = new MessageBuilder(previous);
    }
    messageBuilder.setRetain(flag);
    details.updateMessage(messageBuilder.build());
    updateManager();
  }
}