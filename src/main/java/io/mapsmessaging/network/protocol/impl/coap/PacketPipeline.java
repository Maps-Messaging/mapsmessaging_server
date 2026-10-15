/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.COAP_FAILED_TO_SEND;
import static io.mapsmessaging.network.protocol.impl.coap.Constants.ACK_RANDOM_FACTOR;
import static io.mapsmessaging.network.protocol.impl.coap.Constants.ACK_TIMEOUT;
import static io.mapsmessaging.network.protocol.impl.coap.Constants.MAX_RETRANSMIT;

public class PacketPipeline {

  private final Queue<BasePacket> sendQueue;
  private final Map<Integer, OutstandingExchange> outstandingQueue;
  private final CoapProtocol protocol;

  public PacketPipeline(CoapProtocol protocol) {
    sendQueue = new ConcurrentLinkedQueue<>();
    outstandingQueue = new ConcurrentSkipListMap<>();
    this.protocol = protocol;
  }

  public void close(){
    sendQueue.clear();
    outstandingQueue.values().forEach(OutstandingExchange::cancel);
    outstandingQueue.clear();
  }

  public void send(BasePacket packet) throws IOException {
    if(packet.getType().equals(TYPE.ACK)){
      protocol.send(packet);
    }
    else if (outstandingQueue.isEmpty()) {
      if(!packet.isComplete()){
        sendQueue.offer(packet);
      }
      sendPacket(packet);
    } else {
      sendQueue.offer(packet);
    }
  }

  private void sendPacket(BasePacket basePacket) throws IOException {
    protocol.send(basePacket);
    basePacket.setTimeSent(System.currentTimeMillis());
    if(basePacket.getType().equals(TYPE.CON)){
      OutstandingExchange exchange = new OutstandingExchange(basePacket, initialTimeoutMillis());
      outstandingQueue.put(basePacket.getMessageId(), exchange);
      exchange.schedule();
    }
  }

  private long initialTimeoutMillis() {
    long minimum = ACK_TIMEOUT * 1000L;
    long maximum = (long) (ACK_TIMEOUT * ACK_RANDOM_FACTOR * 1000L);
    return ThreadLocalRandom.current().nextLong(minimum, maximum + 1);
  }

  public void ack(BasePacket ackPacket) throws IOException {
    OutstandingExchange exchange = outstandingQueue.remove(ackPacket.getMessageId());
    if (exchange == null) {
      return;
    }
    exchange.cancel();

    BasePacket sent = exchange.packet;
    sent.sent(ackPacket);
    BasePacket packet;
    if(sent.isComplete()){
      packet = sendQueue.poll();
    }
    else{
      packet = sent;
      packet.setMessageId(protocol.getNextMessageId());
    }
    if (packet != null) {
      sendPacket(packet);
    }
  }

  public void reset(BasePacket resetPacket) throws IOException {
    OutstandingExchange exchange = outstandingQueue.remove(resetPacket.getMessageId());
    if (exchange == null) {
      return;
    }
    exchange.cancel();
    sendNext();
  }

  private void sendNext() throws IOException {
    BasePacket next = sendQueue.poll();
    if (next != null) {
      sendPacket(next);
    }
  }

  private final class OutstandingExchange implements Runnable {

    private final BasePacket packet;
    private long timeoutMillis;
    private int retransmissions;
    private ScheduledFuture<?> future;

    private OutstandingExchange(BasePacket packet, long timeoutMillis) {
      this.packet = packet;
      this.timeoutMillis = timeoutMillis;
      retransmissions = 0;
    }

    private void schedule() {
      future = SimpleTaskScheduler.getInstance().schedule(this, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private void cancel() {
      if (future != null) {
        future.cancel(false);
      }
    }

    @Override
    public void run() {
      if (outstandingQueue.get(packet.getMessageId()) != this) {
        return;
      }
      if (retransmissions >= MAX_RETRANSMIT) {
        outstandingQueue.remove(packet.getMessageId(), this);
        try {
          sendNext();
        } catch (IOException e) {
          protocol.getLogger().log(COAP_FAILED_TO_SEND, packet.getFromAddress(), e);
        }
        return;
      }

      try {
        protocol.send(packet);
        packet.setTimeSent(System.currentTimeMillis());
        retransmissions++;
        timeoutMillis *= 2;
        schedule();
      } catch (IOException e) {
        protocol.getLogger().log(COAP_FAILED_TO_SEND, packet.getFromAddress(), e);
      }
    }
  }
}
