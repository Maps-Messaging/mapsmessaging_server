package io.mapsmessaging.network.io.impl.lora;

import io.mapsmessaging.dto.rest.config.network.impl.LoRaConfigDTO;
import lombok.Getter;

import java.util.concurrent.atomic.LongAdder;

public abstract class LoRaDevice {

  @Getter
  protected final LongAdder packetsSent = new LongAdder();
  @Getter
  protected final LongAdder packetsReceived = new LongAdder();
  @Getter
  protected final LongAdder bytesReceived = new LongAdder();
  @Getter
  protected final LongAdder bytesSent = new LongAdder();

  protected boolean isInitialised = false;

  @Getter
  private LoRaConfigDTO config;


  protected LoRaDevice(LoRaConfigDTO config) {
    this.config = config;
  }

  public abstract void close();

  public String getName() {
    return this.config.getName();
  }


}
