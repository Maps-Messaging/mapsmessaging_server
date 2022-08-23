package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Block extends Option {

  @Getter
  @Setter
  private boolean more;

  @Getter
  @Setter
  private int number;


  @Getter
  @Setter
  private int sizeEx;

  public Block(int id){
    super(id);
  }

  public Block(int id, int number, boolean more, int sizeEx){
    super(id);
    this.number = number;
    this.more = more;
    this.sizeEx = sizeEx;
  }

  @Override
  public void update(byte[] packed) throws IOException{
    if(packed == null || packed.length == 0){
      throw new IOException("Invalid block option must contain at least 1 byte");
    }
    byte last = packed[packed.length -1]; // Get the flags from the end
    sizeEx = last & 0b111;
    more = (last & 0b01000) != 0;
    number = (last & 0xff) >> 4;
    for (int x = 1; x < packed.length; x++) {
      number += ((packed[packed.length - x - 1] & 0xff) << (x * 8 - 4));
    }
  }

  @Override
  public byte[] pack() {
    byte[] buf;
    if(number < 16 ){
      buf = new byte[1];
    }
    else if(number < 4096 ){
      buf = new byte[2];
      buf[0] = (byte)(number & 0xff);
    }
    else{
      buf = new byte[3];
      buf[0] = (byte)(number & 0xff);
      buf[1] = (byte)(number>>8 & 0xff);
    }
    int idx = buf.length -1;
    if(more){
      buf[idx] = (byte)0b1000;
    }
    buf[idx] = (byte)((sizeEx & 0b111) | ((number>>(8*idx)) & 0xf) << 4);
    return buf;
  }
}
