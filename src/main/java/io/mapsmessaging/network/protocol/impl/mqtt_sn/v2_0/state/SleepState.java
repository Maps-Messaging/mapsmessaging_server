/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.SleepManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.BasePublish;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.State;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Disconnect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.PingRequest;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.PingResponse;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Register;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SleepState implements State {

  private final MQTT_SNProtocol protocol;

  private Future<?> reaperRunner;

  private int sleepDuration;


  public SleepState(int sleepDuration, MQTT_SNProtocol protocol) {
    this.sleepDuration = sleepDuration;
    this.protocol = protocol;
    reaperRunner = SimpleTaskScheduler.getInstance().schedule(new Reaper(), sleepDuration, TimeUnit.SECONDS);
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {

    switch (mqtt.getControlPacketId()) {
      case MQTT_SNPacket.CONNECT:
        Connect connect = (Connect)mqtt;
        MQTT_SNPacket response = new ConnAck(ReasonCodes.Success, connect.getSessionExpiry(), session.getName());
        sleepDuration = 0;
        clearReaper();
        sendMessages(0, stateEngine);
        stateEngine.setState(new ConnectedState(response));
        return response;

      case MQTT_SNPacket.DISCONNECT:
        Disconnect disconnect = (Disconnect) mqtt;
        if (disconnect.getExpiry() > 0) {
          sleepDuration = (int)disconnect.getExpiry();
          clearReaper();
        } else {
          mqtt.setCallback(() -> {
            try {
              protocol.close();
            } catch (IOException e) {
              // ignore, we are in a shutdown state here
            }
          });
          stateEngine.setState(new DisconnectedState(mqtt));
        }
        return mqtt;

      case MQTT_SNPacket.PINGREQ:
        if (!reaperRunner.isDone()) {
          clearReaper();
        }
        PingRequest ping = (PingRequest)mqtt;
        int maxMessages = ping.getMaxMessages();
        sendMessages(maxMessages, stateEngine);
        return new PingResponse(protocol.getSleepManager().size());

      default:
        return null;
    }
  }

  private void clearReaper() {
    if (!reaperRunner.isDone()) {
      reaperRunner.cancel(false);
      if (sleepDuration > 0) {
        reaperRunner = SimpleTaskScheduler.getInstance().schedule(new Reaper(), sleepDuration, TimeUnit.SECONDS);
      }
    }
  }

  @Override
  public void sendPublish(MQTT_SNProtocol protocol, String destination, MQTT_SNPacket publish) {
    protocol.getSleepManager().storeEvent(destination,(Publish) publish);
  }

  private void sendMessages(int maxMessages, StateEngine stateEngine) {
    if(maxMessages == 0){
      maxMessages = Integer.MAX_VALUE;
    }
    SleepManager<BasePublish> manager = protocol.getSleepManager();
    if (manager.hasEvents()) {
      Set<String> toSend = manager.getDestinationList();
      for (String destination : toSend) {
        Iterator<BasePublish> iterator = manager.getMessages(destination);
        if(manager.sendRegister(destination)){
          short alias = stateEngine.findTopicAlias(destination);
          Register register = new Register(alias, (short) 0, destination);
          protocol.writeFrame(register);
        }
        while (iterator.hasNext()) {
          Publish publish = (Publish) iterator.next();
          protocol.writeFrame(publish);
          maxMessages--;
          if(maxMessages == 0){
            return;
          }
        }
      }
    }
  }

  public final class Reaper implements Runnable {
    @Override
    public void run() {
      try {
        protocol.close();
      } catch (IOException e) {
        // Ignore, the other side could already have closed
      }
    }
  }
}
