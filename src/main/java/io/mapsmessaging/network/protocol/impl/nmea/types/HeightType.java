package io.mapsmessaging.network.protocol.impl.nmea.types;

import java.util.Locale;

public class HeightType implements Type {

  private final double height;
  private final char unit;

  public HeightType(String value, String unit) {
    height = Double.parseDouble(value);
    this.unit = unit.toUpperCase(Locale.ROOT).charAt(0);
  }

  public char getUnit() {
    return unit;
  }

  public double getHeight() {
    return height;
  }

  @Override
  public String toString() {
    return "" + height+ "," + unit;
  }

}