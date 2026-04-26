package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(
    name = "CanbusConfig",
    description = "Configuration for a CAN bus endpoint. Supports native SocketCAN interfaces and serial CAN adapters."
)
public class CanbusConfigDTO extends EndPointConfigDTO {

  public CanbusConfigDTO() {
    super("canbus");
  }

  @Schema(
      description = "CAN bus device name. For SocketCAN this is usually the Linux interface name, such as can0. For serial adapters this is a logical device name.",
      example = "can0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String deviceName;

  @Schema(
      description = "Serial configuration used when the CAN bus device is accessed through a serial adapter instead of a native SocketCAN interface.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected SerialConfigDTO serialConfig;
}