package io.mapsmessaging.network.io.impl.lora.serial;

import io.mapsmessaging.config.network.impl.LoRaSerialDeviceConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.impl.lora.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.device.LoRaChipDevice;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;
import lombok.Getter;

public class LoRaSerialDevice extends LoRaDevice {
  @Getter
  private LoRaProtocol activeProtocol;

  public LoRaSerialDevice(LoRaSerialDeviceConfig config) {
    super(config);
    activeProtocol = null;
    isInitialised = false;
  }


  @Override
  public void close() {

  }

  public void setProtocol(LoRaProtocol loRaProtocol) {
    activeProtocol = loRaProtocol;
  }
}
