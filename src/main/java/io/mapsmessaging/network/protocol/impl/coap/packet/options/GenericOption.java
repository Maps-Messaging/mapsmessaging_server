package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import lombok.Getter;

public class GenericOption extends Option {

  @Getter
  byte[] value;

  public GenericOption(int id){
    super(id);
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    for(byte b:value){
      int val = b & 0xff;
      if(val < 16){
        sb.append(" 0x0");
      }
      else{
        sb.append(" 0x");
      }
      sb.append(Integer.toHexString(val));
    }
    return super.toString()+"["+sb+"] <"+new String(value)+">";
  }

  @Override
  public void update(byte[] value) {
    this.value = value;
  }

  @Override
  public byte[] pack() {
    return value;
  }
}
