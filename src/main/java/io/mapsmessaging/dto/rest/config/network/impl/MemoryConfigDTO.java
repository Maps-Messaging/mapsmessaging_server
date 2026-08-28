/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Shared-memory/RDMA endpoint configuration")
public class MemoryConfigDTO extends EndPointConfigDTO {
  public MemoryConfigDTO() {
    super("memory");
    serverReadBufferSize = 1024 * 1024;
    serverWriteBufferSize = 1024 * 1024;
  }
}
