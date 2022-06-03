package io.mapsmessaging.network.protocol.impl.z_wave;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.Command;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.ForceSucNodeId;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.GetInitialData;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.NodeProtocolInfo;
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

public class StateManager {

  private final Queue<Command> commandList;
  private final ZWaveProtocol protocol;
  private final PacketFactory packetFactory;
  private final AtomicBoolean runFlag;

  private Command outstanding;
  private int nakCount;


  public StateManager(ZWaveProtocol protocol){
    this.protocol = protocol;
    commandList = new ConcurrentLinkedQueue<>();
    packetFactory = new PacketFactory();
    outstanding = null;
    runFlag = new AtomicBoolean(true);
    nakCount = 0;
    initialiseLink();
    Thread t = new Thread(new CommandThread());
    t.setDaemon(true);
    t.setName(protocol.getName()+":"+protocol.getSessionId());
    t.start();
  }

  public void close(){
    commandList.clear();
    runFlag.set(false);

  }

  private void initialiseLink(){
    queueCommand(new ForceSucNodeId());
    queueCommand(new GetInitialData());
    queueCommand(new NodeProtocolInfo(1));
    queueCommand(new NodeProtocolInfo(4));
    queueCommand(new NodeProtocolInfo(5));
  }

  public void queueCommand(Command command){
    commandList.add(command);
  }

  public void processPacket(Packet packet){
    BasePacket frame = packetFactory.parse(packet);
    if(frame instanceof DataPacket){
      DataPacket dataPacket = (DataPacket) frame;
      if(dataPacket.isValid()){
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

  private void sendCommand(Command command){
    outstanding = command;
    RequestPacket requestPacket = new RequestPacket();
    requestPacket.addCommand(command);
    protocol.writeFrame(requestPacket);
  }

  public final class CommandThread implements Runnable{

    @Override
    public void run() {
      int waitCounter = 0;
      while(runFlag.get()){
        if(outstanding == null) {
          Command command = commandList.poll();
          if (command != null) {
            outstanding = command;
            nakCount = 0;
            waitCounter = 0;
            sendCommand(command);
          }
        }
        else{
          waitCounter++;
          if(waitCounter > 40){
            // 4 seconds has passed and we have no response, lets abort and go the next one
            waitCounter = 0;
            System.err.println("Dropping command :"+outstanding);
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
