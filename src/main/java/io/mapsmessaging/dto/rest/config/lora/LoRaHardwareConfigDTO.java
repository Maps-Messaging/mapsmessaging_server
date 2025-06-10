package io.mapsmessaging.dto.rest.config.lora;

import io.swagger.v3.oas.annotations.media.Schema;

public class LoRaHardwareConfigDTO {

  @Schema(description = "Radio type of the LoRa device", example = "SX1276")
  protected String radio;

  @Schema(description = "Chip Select (CS) pin number", example = "10")
  protected int cs;

  @Schema(description = "IRQ pin number", example = "7")
  protected int irq;

  @Schema(description = "Reset (RST) pin number", example = "3")
  protected int rst;

  @Schema(description = "CAD timeout setting", example = "500")
  protected int cadTimeout;
}
