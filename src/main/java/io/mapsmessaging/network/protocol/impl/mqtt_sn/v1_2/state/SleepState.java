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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListenerFactory;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SleepState implements State {

  private final MQTT_SNProtocol protocol;
  private final PacketListenerFactory packetListenerFactory;

  private Future<?> reaperRunner;

  private int sleepDuration;


  public SleepState(int sleepDuration, MQTT_SNProtocol protocol) {
    this.sleepDuration = sleepDuration;
    packetListenerFactory = new PacketListenerFactory();

    this.protocol = protocol;
    reaperRunner = SimpleTaskScheduler.getInstance().schedule(new Reaper(), sleepDuration, TimeUnit.SECONDS);
  }

  @Override
  public String getName() {
    return "Sleep";
  }


  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) throws MalformedException {

    switch (mqtt.getControlPacketId()) {
      case MQTT_SNPacket.CONNECT:
        MQTT_SNPacket response = new ConnAck(ReasonCodes.SUCCESS);
        sleepDuration = 0;
        clearReaper();
        stateEngine.setState(new ConnectedState(response));
        stateEngine.wake();
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
        stateEngine.emptyQueue(0, () -> protocol.writeFrame(new PingResponse()));
        return null;

      default:
        PacketListener listener = packetListenerFactory.getListener(mqtt.getControlPacketId());
        return listener.handlePacket(mqtt, session, endPoint, protocol, stateEngine);
    }
  }

  @Override
  public void sendPublish(MQTT_SNProtocol protocol, String destination, MQTT_SNPacket publish) {
    protocol.writeFrame(publish);
  }

  private void clearReaper() {
    if (!reaperRunner.isDone()) {
      reaperRunner.cancel(false);
      if (sleepDuration > 0) {
        reaperRunner = SimpleTaskScheduler.getInstance().schedule(new Reaper(), sleepDuration, TimeUnit.SECONDS);
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
