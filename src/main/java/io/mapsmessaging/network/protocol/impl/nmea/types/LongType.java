package io.mapsmessaging.network.protocol.impl.nmea.types;

public class LongType implements Type {

  private final long value;

  public LongType(String value){
    if(value.length() == 0){
      this.value = 0;
    }
    else {
      this.value = Long.parseLong(value);
    }
  }

  public long getValue(){
    return value;
  }


  @Override
  public String toString(){
    return Long.toString(value);
  }
}
