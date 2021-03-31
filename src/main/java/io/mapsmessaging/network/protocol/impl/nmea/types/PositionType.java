package io.mapsmessaging.network.protocol.impl.nmea.types;

import java.util.Locale;

public class PositionType implements Type {

  private final int degrees;
  private final double minutes;
  private final char direction;

  public PositionType(String value, String dir){
    double min = 0;
    int deg = 0;
    char dire = ' ';
    if(value.length() > 0){
      double numeric = Double.parseDouble(value);
      deg = (int)(numeric/100);
      min = (numeric % 100.0f) / 60.0f;
    }
    if(dir.length() > 0){
      dire = dir.toUpperCase(Locale.ROOT).charAt(0);
    }
    minutes = min;
    this.degrees = deg;
    direction = dire;
  }

  public char getDirection(){
    return direction;
  }

  public double getPosition(){
    int indicator = 1;
    if(direction == 'S' || direction == 'W'){
      indicator = -1;
    }
    return (degrees + minutes)*indicator;
  }

  @Override
  public String toString(){
    return ""+((degrees*100)+(minutes * 60))+","+direction;
  }
}
