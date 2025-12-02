package io.mapsmessaging.dto.rest.config.device;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "Serial Device Bus Configuration DTO")
public class SerialBusConfigDTO extends DeviceBusConfigDTO {

  @Schema(description = "Name of the serial bus managemnt", example = "serial")
  protected String name;

  @Schema(description = "List of Serial devices devices on this bus")
  protected List<SerialDeviceDTO> devices;

  @Schema(description = "Trigger mechanism for OneWire bus", example = "temperatureTrigger")
  protected String trigger;

}