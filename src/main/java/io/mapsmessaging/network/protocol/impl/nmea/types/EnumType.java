package io.mapsmessaging.network.protocol.impl.nmea.types;

public class EnumType implements Type {

  private final String id;
  private final String description;

  public EnumType(String id, String description){
    this.id = id;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }


  @Override
  public String toString(){
    return id;
  }
}
