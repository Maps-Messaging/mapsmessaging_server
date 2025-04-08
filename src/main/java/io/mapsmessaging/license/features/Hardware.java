package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Hardware {
  private boolean i2c;
  private boolean spi;
  private boolean oneWire;
}
