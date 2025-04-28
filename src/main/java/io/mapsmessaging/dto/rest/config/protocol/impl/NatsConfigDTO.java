package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "NATS Protocol Configuration DTO")
public class NatsConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Maximum buffer size for NATS", example = "65535")
  protected int maxBufferSize = 65535;

  @Schema(description = "Maximum receive limit for NATS", example = "1000")
  protected int maxReceive = 1000;
}
