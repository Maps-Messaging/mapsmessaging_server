package io.mapsmessaging.network.protocol.impl.nmea.types;

public class StringType implements Type {

  private final String value;

  public StringType(String value){
    this.value = value;
  }

  public String getValue(){
    return value;
  }


  @Override
  public String toString(){
    return value;
  }
}
