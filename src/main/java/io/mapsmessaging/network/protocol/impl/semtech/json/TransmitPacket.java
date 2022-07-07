package io.mapsmessaging.network.protocol.impl.semtech.json;

import lombok.Getter;
import lombok.Setter;

public class TransmitPacket {

  @Getter
  @Setter
  private boolean imme;
  @Getter
  @Setter
  private long tmst;
  @Getter
  @Setter
  private long tmms;
  @Getter
  @Setter
  private float freq;
  @Getter
  @Setter
  private long rfch;
  @Getter
  @Setter
  private long powe;
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
  private long fdev;
  @Getter
  @Setter
  private boolean ipol;
  @Getter
  @Setter
  private long prea;
  @Getter
  @Setter
  private long size;
  @Getter
  @Setter
  private String data;
  @Getter
  @Setter
  private boolean ncrc;
}
