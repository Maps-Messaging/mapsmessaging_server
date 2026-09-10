/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.state.drone.tak;

import static io.mapsmessaging.state.logging.StateLogMessages.COT_STATE_MESSAGE_FAILED;
import static io.mapsmessaging.state.logging.StateLogMessages.COT_STATE_SUBSCRIBER_STARTED;
import static io.mapsmessaging.state.logging.StateLogMessages.COT_STATE_SUBSCRIBER_STOPPED;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.state.MessageHandler;
import io.mapsmessaging.state.StateLoopProtocol;
import io.mapsmessaging.state.util.SessionHelper;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CotStateSubscriber implements MessageHandler, AutoCloseable {

  private final Logger logger = LoggerFactory.getLogger(CotStateSubscriber.class);
  private final StateLoopProtocol protocol;
  private final String inboundTopic;
  private final CotXmlParser parser;
  private final CotTwinUpdater twinUpdater;
  private final CotTaskProfile taskProfile;
  private volatile boolean started;
  private volatile boolean closed;

  public CotStateSubscriber(
      String inboundTopic, CotTwinUpdater twinUpdater, CotTaskProfile taskProfile) {
    this.inboundTopic = Objects.requireNonNull(inboundTopic, "inboundTopic must not be null");
    this.parser = new CotXmlParser();
    this.twinUpdater = Objects.requireNonNull(twinUpdater, "twinUpdater must not be null");
    this.taskProfile = Objects.requireNonNull(taskProfile, "taskProfile must not be null");
    ForwardingMessageHandler forwardingHandler = new ForwardingMessageHandler();
    this.protocol = SessionHelper.createLoopbackProtocol(forwardingHandler);
    forwardingHandler.setDelegate(this);
  }

  CotStateSubscriber(
      StateLoopProtocol protocol,
      String inboundTopic,
      CotXmlParser parser,
      CotTwinUpdater twinUpdater,
      CotTaskProfile taskProfile) {
    this.protocol = Objects.requireNonNull(protocol, "protocol must not be null");
    this.inboundTopic = Objects.requireNonNull(inboundTopic, "inboundTopic must not be null");
    this.parser = Objects.requireNonNull(parser, "parser must not be null");
    this.twinUpdater = Objects.requireNonNull(twinUpdater, "twinUpdater must not be null");
    this.taskProfile = Objects.requireNonNull(taskProfile, "taskProfile must not be null");
  }

  public synchronized void start() throws IOException {
    if (closed) {
      throw new IllegalStateException("CoT state subscriber is closed");
    }
    if (started) {
      return;
    }
    protocol.connect(UUID.randomUUID().toString(), "anonymous", "anonymous");
    protocol.subscribeLocal(
        inboundTopic,
        inboundTopic,
        QualityOfService.AT_MOST_ONCE,
        null,
        null,
        null,
        null,
        null);
    started = true;
    logger.log(COT_STATE_SUBSCRIBER_STARTED, inboundTopic);
  }

  public synchronized void stop() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    if (started) {
      protocol.unsubscribeLocal(inboundTopic);
    }
    protocol.close();
    started = false;
    logger.log(COT_STATE_SUBSCRIBER_STOPPED, inboundTopic);
  }

  @Override
  public void close() throws IOException {
    stop();
  }

  @Override
  public void handle(MessageEvent messageEvent) {
    try {
      if (closed) {
        return;
      }
      Message message = messageEvent.getMessage();
      Map<String, String> metadata = message.getMeta();
      String endpoint = metadata == null ? null : metadata.get("endpointName");
      if (endpoint == null || endpoint.isBlank()) {
        endpoint = messageEvent.getDestinationName();
      }
      byte[] payload = message.getOpaqueData();
      var event = parser.parse(payload);
      if (taskProfile.isTaskEvent(event)) {
        taskProfile.accept(endpoint, event, payload);
      } else if (taskProfile.isTaskStatusEvent(event)) {
        taskProfile.acceptStatus(event);
      } else {
        twinUpdater.update(endpoint, event, Instant.now());
      }
    } catch (IOException | RuntimeException exception) {
      logger.log(COT_STATE_MESSAGE_FAILED, exception, messageEvent.getDestinationName(), exception.getMessage());
    } finally {
      messageEvent.getCompletionTask().run();
    }
  }

  private static final class ForwardingMessageHandler implements MessageHandler {
    private MessageHandler delegate;

    private void setDelegate(MessageHandler delegate) {
      this.delegate = delegate;
    }

    @Override
    public void handle(MessageEvent messageEvent) {
      delegate.handle(messageEvent);
    }
  }
}
