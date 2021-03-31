package io.mapsmessaging.network.protocol.impl.nmea.types;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

public class UTCTimeType implements Type {

  private final OffsetTime time;

  public UTCTimeType(String utcTime){
    double numeric = Double.parseDouble(utcTime);
    int hour = ((int) numeric /10000) % 100;
    int min = ((int) numeric /100) % 100;
    int sec = (int) numeric % 100;
    int nano = (int)(numeric * 1000) % 1000;
    nano = nano * 1000000;
    LocalTime localTime = LocalTime.of(hour, min, sec, nano);
    time = OffsetTime.of(localTime, ZoneOffset.UTC);
  }

  public OffsetTime getTime() {
    return time;
  }

  @Override
  public String toString(){
    return time.toString();
  }

}
