package io.mapsmessaging.dto.rest.config.device;

import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "Serial Bus Configuration DTO")
public class SerialDeviceDTO extends DeviceBusConfigDTO {

  @Schema(description = "Name of the Serial Device", example = "SEN0640")
  protected String name;

  @Schema(description = "Serial port configuration")
  protected SerialConfigDTO serialConfig;

}
