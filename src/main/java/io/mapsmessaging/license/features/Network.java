package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Network {
  private boolean udp;
  private boolean hmac;
  private boolean tcp;
  private boolean ssl;
  private boolean dtls;
  private boolean lora;
  private boolean serial;
  private int maxConnections;
}
