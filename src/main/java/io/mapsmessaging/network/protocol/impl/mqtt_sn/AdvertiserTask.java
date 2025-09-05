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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Advertise;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_ADVERTISER_SENT_PACKET;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_ADVERTISE_PACKET_EXCEPTION;

public class AdvertiserTask implements Runnable {

  private final DatagramSocket socket;
  private final byte[] advertisePacket;
  private final Future<?> future;
  private final InterfaceInformation info;
  private final InetAddress bcast;
  private final int datagramPort;
  private final byte gatewayId;
  private final Logger logger;

  AdvertiserTask(byte gatewayId, EndPoint endPoint, InterfaceInformation info, InetAddress bcast, int interval) throws IOException {
    this.info = info;
    this.bcast = bcast;
    this.gatewayId = gatewayId;
    logger = LoggerFactory.getLogger(AdvertiserTask.class);
    datagramPort = endPoint.getServer().getUrl().getPort();

    socket = new DatagramSocket();
    socket.setReuseAddress(true);
    socket.setBroadcast(true);
    socket.connect(bcast, datagramPort);

    advertisePacket = buildDatagram(interval);
    future = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, interval, interval, TimeUnit.SECONDS);
  }

  private byte[] buildDatagram(int interval) {
    Advertise advertise = new Advertise(gatewayId, (short) interval);
    Packet packet = new Packet(5, false);
    advertise.packFrame(packet);
    packet.flip();
    byte[] tmp = new byte[5];
    packet.get(tmp);
    return tmp;
  }

  public void stop() {
    future.cancel(false);
    socket.close();
  }

  @Override
  public void run() {
    try {
      if (info.isUp()) {
        DatagramPacket packet = new DatagramPacket(advertisePacket, advertisePacket.length, bcast, datagramPort);
        socket.send(packet);
        logger.log(MQTT_SN_ADVERTISER_SENT_PACKET, packet);
      }
    } catch (Exception e) {
      logger.log(MQTT_SN_ADVERTISE_PACKET_EXCEPTION, e);
    }
  }
}