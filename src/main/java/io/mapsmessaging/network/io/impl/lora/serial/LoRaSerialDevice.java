package io.mapsmessaging.network.io.impl.lora.serial;

import io.mapsmessaging.config.network.impl.LoRaSerialDeviceConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.impl.lora.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.device.LoRaChipDevice;

public class LoRaSerialDevice extends LoRaDevice {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public LoRaSerialDevice(LoRaSerialDeviceConfig config) {
    super(config);
    logger = LoggerFactory.getLogger(LoRaChipDevice.class);
    isInitialised = false;
  }


  @Override
  public void close() {

  }

}
