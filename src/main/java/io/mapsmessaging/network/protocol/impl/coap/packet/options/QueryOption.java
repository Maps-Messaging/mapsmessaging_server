package io.mapsmessaging.network.protocol.impl.coap.packet.options;

public class QueryOption extends PackedOption {

  public QueryOption(int id) {
    super(id, "&");
  }

}
