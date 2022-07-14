package io.mapsmessaging.network.protocol.impl.coap.packet.options;

public class Observe extends BinaryOption {

  public Observe() {
    super(Constants.OBSERVE);
  }

  public boolean register(){
    return getValue() == 0;
  }

  @Override
  public String toString(){
    return "Observer( Register:"+register()+")";
  }
}
