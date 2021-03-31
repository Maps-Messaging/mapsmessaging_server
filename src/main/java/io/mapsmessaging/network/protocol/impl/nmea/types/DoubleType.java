package io.mapsmessaging.network.protocol.impl.nmea.types;

public class DoubleType implements Type {

  private final double value;

  public DoubleType(String value){
    if(value.length() > 0) {
      this.value = Double.parseDouble(value);
    }
    else{
      this.value = Double.NaN;
    }
  }

  public double getValue(){
    return value;
  }

  @Override
  public String toString(){
    return Double.toString(value);
  }
}
