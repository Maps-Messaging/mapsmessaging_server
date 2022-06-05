package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import io.mapsmessaging.network.io.Packet;

public class CommandFactory {

  public static Command parseCommand(Packet packet){
    int commandId = packet.get() & 0xff;
    Command command = null;
    switch (commandId){
      case Constants.FUNC_ID_APPLICATION_COMMAND_HANDLER:
        command = new ApplicationCommand();
        break;

      case Constants.FUNC_ID_ZW_APPLICATION_UPDATE:
        command = new ApplicationUpdate();
        break;

      case Constants.FUNC_ID_ZW_SET_SUC_NODE_ID:
        command = new ForceSucNodeId();
        break;

      case Constants.FUNC_ID_SERIAL_API_GET_INIT_DATA:
        command = new GetInitialData();
        break;

      case Constants.FUNC_ID_SERIAL_API_START:
        command = new StartCommand();
        break;

    }

    if(command != null){
      command.unpack(packet);
    }
    return command;
  }

}
