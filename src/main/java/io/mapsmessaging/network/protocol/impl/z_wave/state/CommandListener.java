package io.mapsmessaging.network.protocol.impl.z_wave.state;

import io.mapsmessaging.network.protocol.impl.z_wave.commands.Command;

public interface CommandListener {

  void handleRequest(Command command);
  void handleResponse(Command command);

}
