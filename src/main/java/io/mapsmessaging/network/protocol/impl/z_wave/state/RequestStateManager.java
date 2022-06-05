package io.mapsmessaging.network.protocol.impl.z_wave.state;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.z_wave.ZWaveProtocol;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.Command;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.ForceSucNodeId;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.GetInitialData;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.AckPacket;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.DataPacket;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.NakPacket;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.RequestPacket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestStateManager {

  private final Queue<DataPacket> commandList;
  private final ZWaveProtocol protocol;
  private final PacketFactory packetFactory;
  private final AtomicBoolean runFlag;
  private final CommandListener listener;

  private DataPacket outstanding;
  private int nakCount;

  public RequestStateManager(ZWaveProtocol protocol, CommandListener listener){
    this.protocol = protocol;
    this.listener = listener;
    commandList = new ConcurrentLinkedQueue<>();
    packetFactory = new PacketFactory();
    outstanding = null;
    runFlag = new AtomicBoolean(true);
    nakCount = 0;
    Thread t = new Thread(new CommandThread());
    t.setDaemon(true);
    t.setName(protocol.getName()+":"+protocol.getSessionId());
    t.start();
  }

  public void close(){
    commandList.clear();
    runFlag.set(false);
  }

  public void queueResponse(Command command){
    RequestPacket requestPacket = new RequestPacket();
    requestPacket.addCommand(command);
    commandList.add(requestPacket);
  }

  public void queueRequest(Command command){
    RequestPacket requestPacket = new RequestPacket();
    requestPacket.addCommand(command);
    commandList.add(requestPacket);
  }


  public void processPacket(Packet packet){
    BasePacket frame = packetFactory.parse(packet);
    if(frame instanceof DataPacket){
      DataPacket dataPacket = (DataPacket) frame;
      if(dataPacket.isValid()){
        if(dataPacket instanceof RequestPacket){
          for(Command command:dataPacket.getCommandList()){
            listener.handleRequest(command);
          }
        }
        else{
          for(Command command:dataPacket.getCommandList()){
            listener.handleResponse(command);
          }
        }

        protocol.writeFrame( new AckPacket());
      }
      else {
        protocol.writeFrame(new NakPacket());
      }
    }
    if(frame instanceof AckPacket){
      sendNext();
    }
    else if(frame instanceof NakPacket){
      if(outstanding != null) {
        if(nakCount <4) {
          sendCommand(outstanding);
          nakCount++;
        }
        else{
          sendNext();
        }
      }
    }
  }

  private void sendNext(){
    outstanding = null;
  }

  private void sendCommand(DataPacket data){
    System.err.println("Sending :->"+data);
    protocol.writeFrame(data);
  }

  public final class CommandThread implements Runnable{

    @Override
    public void run() {
      int waitCounter = 0;
      while(runFlag.get()){
        if(outstanding == null) {
          DataPacket data = commandList.poll();
          if (data != null) {
            outstanding = data;
            nakCount = 0;
            waitCounter = 0;
            sendCommand(data);
          }
        }
        else{
          waitCounter++;
          if(waitCounter > 40){
            waitCounter = 0;
            outstanding = null;
          }
        }
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          // We have been interrupted, seems we need to exit
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

}
