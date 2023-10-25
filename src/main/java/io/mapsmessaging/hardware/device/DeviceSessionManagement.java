/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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
import io.mapsmessaging.hardware.device.filter.DataFilter;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.schemas.config.SchemaConfig;
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
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Data
public class DeviceSessionManagement implements Runnable, MessageListener {
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

  private byte[] previousPayload;
  private ProtocolMessageTransformation transformation;
  private Future<?> scheduledFuture;

  public DeviceSessionManagement(DeviceHandler deviceHandler, String topicNameTemplate, DataFilter filter, BusHandler busHandler, String selector){
    this.device = deviceHandler;
    this.topicNameTemplate = topicNameTemplate;
    this.filter = filter;
    this.busHandler = busHandler;
    scheduledFuture = null;
    previousPayload = null;
    device.getController().setRaiseExceptionOnError(true);
    ParserExecutor executor = null;
    if(selector != null && !selector.isBlank()){
      try {
        executor = SelectorParser.compile(selector);
      } catch (ParseException e) {
        // ToDo
      }
    }
    parser = executor;
  }

  public void start() throws ExecutionException, InterruptedException {
    destination = session.findDestination(device.getTopicName(topicNameTemplate+"/data"), DestinationType.TOPIC).get();
    if(device.enableRaw()) {
      raw = session.findDestination(device.getTopicName(topicNameTemplate+ "/raw"), DestinationType.TOPIC).get();
    }
    try {
      SchemaConfig schemaConfig = device.getSchema();
      if(schemaConfig != null) {
        schemaConfig.setUniqueId(UUID.randomUUID());
        destination.updateSchema(schemaConfig, null);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      // todo
    }
    if(device.enableConfig()) {
      config = session.findDestination(device.getTopicName(topicNameTemplate+ "/config"), DestinationType.TOPIC).get();
      try {
        SubscriptionContextBuilder subscriptionContextBuilder = new SubscriptionContextBuilder(config.getFullyQualifiedNamespace(), ClientAcknowledgement.AUTO);
        subscriptionContextBuilder.setQos(QualityOfService.AT_MOST_ONCE);
        subscriptionContextBuilder.setNoLocalMessages(true);
        subscribedEventManager = session.addSubscription(subscriptionContextBuilder.build());
        updateConfig();
      } catch (Throwable e) {
        e.printStackTrace();
        // To Do
      }
    }
    device.getTrigger().addTask(this);
  }

  public void stop() throws IOException {
    device.getTrigger().removeTask(this);
    if(config != null) {
      session.removeSubscription(config.getFullyQualifiedNamespace());
    }
    SessionManager.getInstance().close(session, true);
  }

  public String getName() {
    return device.getName();
  }

  private Message buildMessage(byte[] payload, boolean retain) {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("busName", device.getBusName());
    if(device.getBusNumber()>=0) meta.put("busNumber", ""+device.getBusNumber());
    meta.put("version", device.getVersion());
    meta.put("device", device.getName());
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(payload);
    messageBuilder.setTransformation(transformation);
    messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
    messageBuilder.setMeta(meta);
    messageBuilder.setRetain(retain);
    return messageBuilder.build();
  }

  @Override
  public void run() {
    byte[] payload;
    try {
      payload = device.getData();
      if(parser != null){
        JSONObject jsonObject = new JSONObject(new String(payload));
        Map<String, Object> map = jsonObject.toMap();
        if(!parser.evaluate(map)){
          return;
        }
      }
    } catch (IOException e) {
      try {
        stop(); // remove and close session
        busHandler.closedSession(this);
      } catch (IOException ex) {

      }
      // OK, device is offline
      payload = null;
    }

    if(payload != null) {
      try {
        if (filter.send(previousPayload, payload)) {
          destination.storeMessage(buildMessage(payload, true));
          previousPayload = payload;
        }
      } catch (IOException e) {
        // We have bigger issues here
      }
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    try {
      byte[] update = device.updateConfig(messageEvent.getMessage().getOpaqueData());
      if(update != null) {
        Thread t = new Thread(() -> {
          try {
            config.storeMessage(buildMessage(update, true));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
        t.start();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void updateConfig() {
    try {
      byte[] update = device.getConfiguration();
      if(update != null) {
        config.storeMessage(buildMessage(update, true));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
