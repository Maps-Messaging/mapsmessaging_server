package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;

public abstract class Option {

  @Getter
  @Setter
  private int id;

  protected Option(int id){
    this.id = id;
  }

  @Override
  public String toString(){
    return "Id:"+id;
  }

  public abstract void update(byte[] value) throws IOException;

  public abstract byte[] pack();

}
