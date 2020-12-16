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

package org.maps.messaging.engine.session.will;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.ThreadContext;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.MessageDaemon;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.utilities.threads.SimpleTaskScheduler;

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

  public void run() {
    try {
      if (active) {
        ThreadContext.put("session", details.getSessionId());
        ThreadContext.put("protocol", details.getProtocol());
        ThreadContext.put("version", details.getVersion());

        logger.log(LogMessages.WILL_TASK_SENDING, details.getMsg());
        DestinationImpl dest = MessageDaemon.getInstance().getDestinationManager().find(details.getDestination());
        if (dest != null) {
          dest.storeMessage(details.getMsg());
        }
      }
      WillTaskManager.getInstance().remove(details.getSessionId());
    } catch (IOException e) {
      logger.log(LogMessages.WILL_TASK_EXCEPTION, e, details.getMsg().toString());
    } finally {
      ThreadContext.clearMap();
    }
  }

  public void updateTopic(String topic) {
    details.updateTopic(topic);
    updateManager();
  }

  public void updateQoS(QualityOfService qos) {
    MessageBuilder messageBuilder = new MessageBuilder(details.getMsg());
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
    WillTaskManager.getInstance().remove(details.getSessionId());
    WillTaskManager.getInstance().put(details.getSessionId(), details);
  }

  @Override
  public String toString() {
    return "WillTask:Active:" + active + " " + details.toString();
  }

}