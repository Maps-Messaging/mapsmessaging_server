package io.mapsmessaging.dto.rest.config.protocol.impl;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "LoRa Gateway Configuration DTO")
public class LoraGatewayConfigDTO extends MqttSnConfigDTO {

  @Schema(description = "Address to use for the gateway", example = "10")
  protected int address = 2;

  @Schema(description = "Power to configure the radio to use", example = "22")
  protected int power = 22;

  @Schema(description = "Hex encoded key to use", example = "0xabcdef0123456789")
  protected String hexKeyString = "";
}
