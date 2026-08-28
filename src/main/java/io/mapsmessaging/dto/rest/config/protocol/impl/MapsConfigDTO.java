/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "MAPS native protocol configuration DTO")
public class MapsConfigDTO extends ProtocolConfigDTO {

  public MapsConfigDTO() {
    super("maps");
  }

  @Schema(description = "MAPS keep-alive interval in seconds", example = "30", defaultValue = "30", minimum = "1")
  protected int keepAlive = 30;

  @Schema(description = "Maximum accepted MAPS frame size in bytes", example = "67108864", defaultValue = "67108864", minimum = "1024")
  protected int maximumFrameSize = 64 * 1024 * 1024;

  @Schema(description = "Maximum number of in-flight acknowledged messages", example = "1024", defaultValue = "1024", minimum = "1")
  protected int receiveMaximum = 1024;
}
