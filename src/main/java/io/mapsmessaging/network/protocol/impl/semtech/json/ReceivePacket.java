package io.mapsmessaging.network.protocol.impl.semtech.json;

import lombok.Getter;
import lombok.Setter;

public class ReceivePacket {
  @Getter
  @Setter
  private String time;
  @Getter
  @Setter
  private long tmms;
  @Getter
  @Setter
  private long tmst;
  @Getter
  @Setter
  private double freq;
  @Getter
  @Setter
  private long chan;
  @Getter
  @Setter
  private long rfch;
  @Getter
  @Setter
  private long stat;
  @Getter
  @Setter
  private String modu;
  @Getter
  @Setter
  private String datr_s;
  @Getter
  @Setter
  private long datr;
  @Getter
  @Setter
  private String codr;
  @Getter
  @Setter
  private long rssi;
  @Getter
  @Setter
  private double lsnr;
  @Getter
  @Setter
  private long size;
  @Getter
  @Setter
  private String data;
}

