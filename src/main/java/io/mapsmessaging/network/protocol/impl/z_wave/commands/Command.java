package io.mapsmessaging.network.protocol.impl.z_wave.commands;

import io.mapsmessaging.network.io.Packet;

public abstract class Command {

  public abstract int getCommand();

  public void unpack(Packet packet) {
    System.err.println("Unprocessed::"+packet);
  }

  public void pack(Packet packet){
    packet.putByte(getCommand());
  }

}
