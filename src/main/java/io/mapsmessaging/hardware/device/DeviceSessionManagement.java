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

package io.mapsmessaging.hardware.device;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.devices.DeviceType;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.hardware.device.filter.DataFilter;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.security.uuid.UuidGenerator;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.mapsmessaging.logging.ServerLogMessages.*;

@Data
public class DeviceSessionManagement implements Runnable, MessageListener {
  private static Logger logger = LoggerFactory.getLogger(DeviceSessionManagement.class);
  private final DeviceHandler device;
  private final String topicNameTemplate;
  private final DataFilter filter;
  private final BusHandler busHandler;
  private final ParserExecutor parser;

  private Session session;
  private Destination destination;
  private Destination config;
  private Destination raw;
  private SubscribedEventManager subscribedEventManager;
  private SubscribedEventManager displayEventManager;

  private byte[] previousPayload;
  private ProtocolMessageTransformation transformation;

  public DeviceSessionManagement(DeviceHandler deviceHandler, String topicNameTemplate, DataFilter filter, BusHandler busHandler, String selector){
    this.device = deviceHandler;
    this.topicNameTemplate = topicNameTemplate;
    this.filter = filter;
    this.busHandler = busHandler;
    previousPayload = null;
    device.getController().setRaiseExceptionOnError(true);
    ParserExecutor executor = null;
    if(selector != null && !selector.isBlank()){
      try {
        executor = SelectorParser.compile(selector);
      } catch (ParseException e) {
        logger.log(DEVICE_SELECTOR_PARSER_EXCEPTION, selector, e);
      }
    }
    parser = executor;
  }

  public void start() throws ExecutionException, InterruptedException {
    logger.log(DEVICE_START, device.getName() );

    destination = session.findDestination(device.getTopicName(topicNameTemplate+"/data"), DestinationType.TOPIC).get();
    if(device.enableRaw()) {
      raw = session.findDestination(device.getTopicName(topicNameTemplate+ "/raw"), DestinationType.TOPIC).get();
    }
    try {
      SchemaConfig schemaConfig = device.getSchema();
      if(schemaConfig != null) {
        SchemaManager manager = SchemaManager.getInstance();
        boolean found = manager.getAll().stream()
            .anyMatch(configured -> configured.getSource().equalsIgnoreCase(schemaConfig.getSource()));
        if(!found) {
          schemaConfig.setUniqueId(UuidGenerator.getInstance().generate());
          destination.updateSchema(schemaConfig, null);
        }
      }
    } catch (Exception e) {
      logger.log(DEVICE_SCHEMA_UPDATE_EXCEPTION, e);

    }
    if(device.enableConfig()) {
      config = session.findDestination(device.getTopicName(topicNameTemplate+ "/config"), DestinationType.TOPIC).get();
      try {
        updateConfig();
        subscribedEventManager = subscribe(config);
      } catch (Exception e) {
        logger.log(DEVICE_SUBSCRIPTION_EXCEPTION, e);
      }
    }
    switch(device.getType()){
      case SENSOR:
      case CLOCK:
        device.getTrigger().addTask(this);
        break;

      case DISPLAY:
        try {
          displayEventManager = subscribe(destination);
        } catch (Exception e) {
          logger.log(DEVICE_SUBSCRIPTION_EXCEPTION, e);
        }
        break;

      default:
        break;

    }
  }

  private SubscribedEventManager subscribe(Destination destination) throws IOException {
    SubscriptionContextBuilder subscriptionContextBuilder = new SubscriptionContextBuilder(destination.getFullyQualifiedNamespace(), ClientAcknowledgement.AUTO);
    subscriptionContextBuilder.setQos(QualityOfService.AT_MOST_ONCE);
    subscriptionContextBuilder.setNoLocalMessages(true);
    return session.addSubscription(subscriptionContextBuilder.build());
  }

  public void stop() throws IOException {
    logger.log(DEVICE_STOP, device.getName() );
    device.getTrigger().removeTask(this);
    if(config != null) {
      session.removeSubscription(config.getFullyQualifiedNamespace());
    }
    SessionManager.getInstance().close(session, true);
  }

  public String getName() {
    return device.getName();
  }

  private Message buildMessage(byte[] payload) {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("busName", device.getBusName());
    if(device.getBusNumber()>=0) meta.put("busNumber", ""+device.getBusNumber());
    meta.put("version", device.getVersion());
    meta.put("device", device.getName());
    meta.put("sessionId", session.getName());
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(payload);
    messageBuilder.setTransformation(transformation);
    messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
    messageBuilder.setMeta(meta);
    messageBuilder.setRetain(true);
    return messageBuilder.build();
  }

  @Override
  public void run() {
    byte[] payload;
    try {
      payload = device.getData();
      boolean send = false;
      if (filter.send(previousPayload, payload)) {
        if (parser != null) {
          JSONObject jsonObject = new JSONObject(new String(payload));
          Map<String, Object> map = jsonObject.toMap();
          if (parser.evaluate(map)) {
            send = true;
          }
        }
        else{
          send = true;
        }
      }
      if(!send) return;
    } catch (IOException e) {
      logger.log(DEVICE_PUBLISH_EXCEPTION, e);
      try {
        stop(); // remove and close session
        busHandler.closedSession(this);
      } catch (IOException ex) {
        // Ignore we are closing this session
      }
      // OK, device is offline
      payload = null;
    }

    if(payload != null) {
      try {
        destination.storeMessage(buildMessage(payload));
        previousPayload = payload;
      } catch (IOException e) {
        logger.log(DEVICE_PUBLISH_EXCEPTION, e);
      }
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    try {
      byte[] update = device.updateConfig(messageEvent.getMessage().getOpaqueData());
      if(update != null && device.getType() == DeviceType.SENSOR) {
        Thread t = new Thread(() -> {
          try {
            config.storeMessage(buildMessage(update));
          } catch (IOException e) {
            logger.log(DEVICE_PUBLISH_EXCEPTION, e);
          }
        });
        t.start();
      }
    } catch (Throwable e) {
      logger.log(DEVICE_PUBLISH_EXCEPTION, e);
    }
    messageEvent.getCompletionTask().run();
  }

  public void updateConfig() {
    try {
      byte[] update = device.getConfiguration();
      if(update != null) {
        config.storeMessage(buildMessage(update));
      }
    } catch (IOException e) {
      logger.log(DEVICE_PUBLISH_EXCEPTION, e);
    }
  }

}
