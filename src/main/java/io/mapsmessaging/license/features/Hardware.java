package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Hardware {
  private boolean i2cSupported;
  private boolean spiSupported;
  private boolean oneWireSupported;
}
