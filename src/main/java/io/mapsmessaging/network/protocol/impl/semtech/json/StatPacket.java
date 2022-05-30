package io.mapsmessaging.network.protocol.impl.semtech.json;

import lombok.Getter;
import lombok.Setter;

public class StatPacket {

  @Getter
  @Setter
  private String time;
  @Getter
  @Setter
  private double lati;
  @Getter
  @Setter
  private double longitude;
  @Getter
  @Setter
  private long alti;
  @Getter
  @Setter
  private long rxnb;
  @Getter
  @Setter
  private long rxok;
  @Getter
  @Setter
  private long rxfw;
  @Getter
  @Setter
  private double ackr;
  @Getter
  @Setter
  private long dwnb;
  @Getter
  @Setter
  private long txnb;
}

