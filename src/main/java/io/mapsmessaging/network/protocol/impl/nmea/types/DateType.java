package io.mapsmessaging.network.protocol.impl.nmea.types;


import java.time.LocalDate;

public class DateType implements Type {

  private final LocalDate localDate;

  public DateType(String date){
    int numeric = Integer.parseInt(date);
    int day = (numeric / 10000) % 100;
    int month = ( numeric / 100) % 100;
    int year = numeric % 100 + 2000;

    localDate = LocalDate.of(year, month, day);
  }

  public LocalDate getDate() {
    return localDate;
  }

  @Override
  public String toString(){
    return localDate.toString();
  }

}