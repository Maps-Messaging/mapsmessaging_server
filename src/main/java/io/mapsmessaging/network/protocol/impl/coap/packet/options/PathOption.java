package io.mapsmessaging.network.protocol.impl.coap.packet.options;

public class PathOption extends PackedOption {

  public PathOption(int id) {
    super(id, "/");
  }
}
