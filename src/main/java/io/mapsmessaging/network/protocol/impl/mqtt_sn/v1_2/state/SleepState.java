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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.SleepManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.BasePublish;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Disconnect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PingResponse;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Register;
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
        MQTT_SNPacket response = new ConnAck(ReasonCodes.Success);
        sleepDuration = 0;
        clearReaper();
        sendMessages(stateEngine);
        stateEngine.setState(new ConnectedState(response));
        return response;

      case MQTT_SNPacket.DISCONNECT:
        Disconnect disconnect = (Disconnect) mqtt;
        if (disconnect.getDuration() > 0) {
          sleepDuration = disconnect.getDuration();
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
        sendMessages(stateEngine);
        return new PingResponse();

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
    protocol.getSleepManager().storeEvent(destination, (Publish)publish);
  }

  private void sendMessages(StateEngine stateEngine) {
    SleepManager<BasePublish> manager = protocol.getSleepManager();
    if (manager.hasEvents()) {
      Set<String> toSend = manager.getDestinationList();
      for (String destination : toSend) {
        if(manager.sendRegister(destination)){
          short alias = stateEngine.findTopicAlias(destination);
          Register register = new Register(alias, (short) 0, destination);
          protocol.writeFrame(register);
        }
        Iterator<BasePublish> iterator = manager.getMessages(destination);
        while (iterator.hasNext()) {
          Publish publish = (Publish) iterator.next();
          protocol.writeFrame(publish);
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
