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
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.schemas.config.SchemaConfig;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Data
public class DeviceSessionManagement implements Runnable, MessageListener {
  private final DeviceHandler device;
  private Session session;
  private Destination destination;
  private Destination config;
  private Destination raw;
  private SubscribedEventManager subscribedEventManager;

  private ProtocolMessageTransformation transformation;
  private Future<?> scheduledFuture;

  public DeviceSessionManagement(DeviceHandler deviceHandler){
    this.device = deviceHandler;
    scheduledFuture = null;
  }

  public void start() throws ExecutionException, InterruptedException {
    destination = session.findDestination("/device/"+device.getBusName()+"/"+device.getName()+"/data", DestinationType.TOPIC).get();
    if(device.enableRaw()) {
      raw = session.findDestination("/device/" + device.getBusName() + "/" + device.getName() + "/raw", DestinationType.TOPIC).get();
    }
    try {
      SchemaConfig schemaConfig = device.getSchema();
      if(schemaConfig != null) {
        destination.updateSchema(schemaConfig, null);
      }
    } catch (IOException e) {
      // todo
    }
    if(device.enableConfig()) {
      config = session.findDestination("/device/" + device.getBusName() + "/" + device.getName() + "/config", DestinationType.TOPIC).get();
      try {
        SubscriptionContextBuilder subscriptionContextBuilder = new SubscriptionContextBuilder(config.getFullyQualifiedNamespace(), ClientAcknowledgement.AUTO);
        subscriptionContextBuilder.setQos(QualityOfService.AT_MOST_ONCE);
        subscriptionContextBuilder.setNoLocalMessages(true);
        subscribedEventManager = session.addSubscription(subscriptionContextBuilder.build());
      } catch (IOException e) {
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
    meta.put("bus", device.getBusName());
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
    try {
      destination.storeMessage(buildMessage(device.getData(), true));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    try {
      byte[] update = device.updateConfig(messageEvent.getMessage().getOpaqueData());
      if(update != null) {
        config.storeMessage(buildMessage(update, false));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
