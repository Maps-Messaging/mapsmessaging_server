package io.mapsmessaging.network.protocol.impl.nmea.types;

public class BooleanType  implements Type {

  private final boolean value;

  public BooleanType(String value, String check){
    this.value = value.equalsIgnoreCase(check);
  }

  public boolean getValue(){
    return value;
  }
  
  @Override
  public String toString(){
    return Boolean.toString(value);
  }
}
