package io.mapsmessaging.network.protocol.impl.z_wave.state;

import static io.mapsmessaging.network.protocol.impl.z_wave.commands.Constants.FUNC_ID_SERIAL_API_START;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.z_wave.ZWaveProtocol;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.Command;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.ForceSucNodeId;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.GetInitialData;
import io.mapsmessaging.network.protocol.impl.z_wave.commands.StartCommand;
import java.util.concurrent.TimeUnit;

public class NetworkStateManager implements CommandListener{

  private final RequestStateManager requestStateManager;

  public NetworkStateManager(ZWaveProtocol protocol){
    requestStateManager = new RequestStateManager(protocol, this);
    Thread t = new Thread(() -> {
      try {
        TimeUnit.SECONDS.sleep(5);
        initialiseLink();
      } catch (InterruptedException e) {
      }
    });
    t.start();
  }

  public void close(){
    requestStateManager.close();
  }

  public void processPacket(Packet packet) {
    requestStateManager.processPacket(packet);
  }

  private void initialiseLink(){
    requestStateManager.queueRequest(new ForceSucNodeId());
    requestStateManager.queueRequest(new GetInitialData());
  }

  public void handleResponse(Command command){
    System.err.println("Response Command:"+command);
  }

  public void handleRequest(Command command){
    System.err.println("Request Command:"+command);
    if(command.getCommand() == FUNC_ID_SERIAL_API_START){
      requestStateManager.queueResponse(new StartCommand());
    }
  }

}
