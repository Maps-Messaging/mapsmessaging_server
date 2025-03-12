package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Protocols {
  private boolean mqtt;
  private boolean amqp;
  private boolean mqtt_sn;
  private boolean coap;
  private boolean nmea_0183;
  private boolean semtech;
  private boolean extensions;
  private boolean stomp;
  private boolean restApi;
}
