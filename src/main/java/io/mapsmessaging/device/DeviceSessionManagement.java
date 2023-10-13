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

package io.mapsmessaging.device;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.device.handler.DeviceHandler;
import io.mapsmessaging.devices.DeviceType;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Data
public class DeviceSessionManagement implements Runnable, MessageListener {
  private final DeviceHandler device;
  private Session session;
  private Destination destination;

  private ProtocolMessageTransformation transformation;
  private Future<?> scheduledFuture;

  public DeviceSessionManagement(DeviceHandler deviceHandler){
    this.device = deviceHandler;
    scheduledFuture = null;
  }

  public void start() throws ExecutionException, InterruptedException {
    destination = session.findDestination("/device/"+device.getBusName()+"/"+device.getName(), DestinationType.TOPIC).get();
    if(device.getController().getType() == DeviceType.SENSOR){
      scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 1, 30, TimeUnit.SECONDS);
    }

  }

  public void stop() throws IOException {
    if(scheduledFuture != null){
      scheduledFuture.cancel(true);
      SessionManager.getInstance().close(session, true);
    }
  }

  public String getName() {
    return device.getName();
  }

  private Message buildMessage() throws IOException {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("bus", device.getBusName());
    meta.put("version", device.getVersion());
    meta.put("device", device.getName());
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(device.getData());
    messageBuilder.setTransformation(transformation);
    messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
    messageBuilder.setMeta(meta);
    return messageBuilder.build();
  }

  @Override
  public void run() {
    try {
      destination.storeMessage(buildMessage());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }
}
