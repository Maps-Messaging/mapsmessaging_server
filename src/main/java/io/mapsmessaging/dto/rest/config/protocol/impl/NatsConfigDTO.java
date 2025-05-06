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

  @Schema(description = "Enable NATS Streams via jetstream", example = "true")
  protected boolean enableStreams = false;

  @Schema(description = "Enable NATS Key values via jetstream", example = "true")
  protected boolean enableKeyValues = false;

  @Schema(description = "Enable NATS object store via jetstream", example = "true")
  protected boolean enableObjectStore = false;

  @Schema(description = "Ping timeout in milliseconds", example = "60000")
  protected int keepAlive = 60_000;

  @Schema(description = "Root for the NATS streams", example = "/nats", defaultValue = "")
  protected String namespaceRoot = "";

  @Schema(description = "Enable or disable stream deletion", example = "true")
  protected boolean enableStreamDelete = true;

}
