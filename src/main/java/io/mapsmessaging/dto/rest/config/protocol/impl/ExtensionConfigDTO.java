package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper=false)
public class ExtensionConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Map of config entries")
  protected Map<String, Object> config;
}
