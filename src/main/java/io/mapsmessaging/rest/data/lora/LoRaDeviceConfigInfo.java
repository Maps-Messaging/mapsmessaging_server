package io.mapsmessaging.rest.data.lora;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class LoRaDeviceConfigInfo {

  private String name;
  private String radio;

  private int cs;
  private int irq;
  private int rst;
  private int power;
  private int cadTimeout;
  private float frequency;

}