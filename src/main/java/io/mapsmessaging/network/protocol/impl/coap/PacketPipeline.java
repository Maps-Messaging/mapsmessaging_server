package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PacketPipeline {

  private final Queue<BasePacket> sendQueue;
  private final Map<Integer, BasePacket> outstandingQueue;
  private final CoapProtocol protocol;
  private final ScheduledFuture<?> retransmissionThread;

  public PacketPipeline(CoapProtocol protocol) {
    sendQueue = new ConcurrentLinkedQueue<>();
    outstandingQueue = new ConcurrentSkipListMap<>();
    this.protocol = protocol;
    retransmissionThread = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new RetransmissionThread(),2,2, TimeUnit.SECONDS);
  }

  public void close(){
    sendQueue.clear();
    outstandingQueue.clear();
    retransmissionThread.cancel(true);
  }

  public void send(BasePacket packet) throws IOException {
    if(packet.getType().equals(TYPE.ACK)){
      protocol.send(packet);
    }
    else if (outstandingQueue.isEmpty()) {
      sendPacket(packet);
    } else {
      sendQueue.offer(packet);
    }
  }

  private void sendPacket(BasePacket basePacket) throws IOException {
    if(basePacket.getType().equals(TYPE.CON)){
      outstandingQueue.put(basePacket.getMessageId(), basePacket);
    }
    protocol.send(basePacket);
    basePacket.setTimeSent(System.currentTimeMillis());
  }

  public void ack(int messageId) throws IOException {
    outstandingQueue.remove(messageId);
    BasePacket packet = sendQueue.poll();
    if (packet != null) {
      sendPacket(packet);
    }
  }

  private final class RetransmissionThread implements Runnable{


    @Override
    public void run() {
      long now = System.currentTimeMillis();
      for(BasePacket packet: outstandingQueue.values()){
        if(packet.getTimeSent() < (now- 2000)){
          if(packet.incrementResendCount() < 4){
            try {
              send(packet);
            } catch (IOException e) {

            }
            packet.setTimeSent(now);
          }
          else{
            outstandingQueue.remove(packet.getMessageId());
            break;
          }
        }
      }
    }
  }



}