package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class CanbusConfigDTO extends EndPointConfigDTO {

  public CanbusConfigDTO() {
    super("canbus");
  }


  @Schema(
      description = "Canbus device name",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String deviceName;

}
