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
  private int size;


  @Getter
  @Setter
  private int sizeEx;

  public Block(int id){
    super(id);
  }


  @Override
  public void update(byte[] packed) throws IOException{
    if(packed == null || packed.length == 0){
      throw new IOException("Invalid block option must contain at least 1 byte");
    }
    byte last = packed[packed.length -1]; // Get the flags from the end
    sizeEx = last & 0b111;
    more = (last & 0b01000) != 0;
    size = (last & 0xff) >> 4;
    for (int x = 1; x < packed.length; x++) {
      size += ((packed[packed.length - x - 1] & 0xff) << (x * 8 - 4));
    }
  }
}
